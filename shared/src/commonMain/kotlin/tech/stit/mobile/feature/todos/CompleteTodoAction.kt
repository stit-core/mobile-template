package tech.stit.mobile.feature.todos

import tech.stit.mobile.core.api.toMessageKey
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.auth.signedIn
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.core.lib.ConnectivityMonitor
import tech.stit.mobile.core.lib.attempt
import tech.stit.mobile.entity.todo.TodoRepository

// Кнопка «Готово» виджета и уведомления: одна оркестрация на все каналы, наружу уходит только результат.
class CompleteTodoAction internal constructor(
    private val todos: TodoRepository,
    private val session: SessionStore,
    private val connectivity: ConnectivityMonitor,
    private val widget: TodayTodosWidgetProvider,
) {
    suspend fun run(todoId: String): CompleteTodoResult {
        val result = attempt { complete(todoId) }.getOrElse { CompleteTodoResult.Failed(it.toMessageKey()) }
        // Снимок виджета живёт вне процесса приложения: после отметки его обновляет само действие, а не экран.
        if (result is CompleteTodoResult.Done || result is CompleteTodoResult.AlreadyDone) attempt { widget.publish() }
        return result
    }

    private suspend fun complete(todoId: String): CompleteTodoResult = when {
        !session.signedIn() -> CompleteTodoResult.SignedOut

        // Офлайн-очереди у записи нет: обещать закрытие задачи без сети нечем.
        !connectivity.online.value -> CompleteTodoResult.Failed(MessageKey.NetworkError)

        else -> todos.get(todoId).let { todo ->
            // Отметка идемпотентна: закрытую задачу повторное нажатие не открывает заново.
            if (todo.done) CompleteTodoResult.AlreadyDone(todo.title) else CompleteTodoResult.Done(todos.toggle(todoId).title)
        }
    }
}

// Что канал показывает пользователю в ответ; текст берётся из ресурсов платформы по этому результату.
sealed class CompleteTodoResult {
    data class Done(val title: String) : CompleteTodoResult()

    data class AlreadyDone(val title: String) : CompleteTodoResult()

    data object SignedOut : CompleteTodoResult()

    data class Failed(val error: MessageKey) : CompleteTodoResult()
}
