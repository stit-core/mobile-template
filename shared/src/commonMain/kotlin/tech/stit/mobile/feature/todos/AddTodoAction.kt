package tech.stit.mobile.feature.todos

import tech.stit.mobile.core.api.toMessageKey
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.auth.signedIn
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.core.lib.ConnectivityMonitor
import tech.stit.mobile.core.lib.attempt
import tech.stit.mobile.entity.todo.TodoRepository

// Задача, заведённая голосом или автоматизацией: приложение не открывается, ответ — подтверждение с заголовком.
class AddTodoAction internal constructor(
    private val todos: TodoRepository,
    private val session: SessionStore,
    private val connectivity: ConnectivityMonitor,
    private val widget: TodayTodosWidgetProvider,
) {
    suspend fun run(text: String): AddTodoResult {
        val result = attempt { add(text) }.getOrElse { AddTodoResult.Failed(it.toMessageKey()) }
        // Снимок виджета живёт вне процесса приложения: после заведения задачи его обновляет само действие.
        if (result is AddTodoResult.Added) attempt { widget.publish() }
        return result
    }

    private suspend fun add(text: String): AddTodoResult {
        val title = text.trim()
        return when {
            !session.signedIn() -> AddTodoResult.SignedOut

            title.isEmpty() -> AddTodoResult.Failed(MessageKey.ValidationError)

            // Офлайн-очереди у записи нет: ассистенту честнее сказать про связь, чем потерять задачу.
            !connectivity.online.value -> AddTodoResult.Failed(MessageKey.NetworkError)

            else -> AddTodoResult.Added(todos.create(title).title)
        }
    }
}

// Что канал показывает пользователю в ответ; текст берётся из ресурсов платформы по этому результату.
sealed class AddTodoResult {
    data class Added(val title: String) : AddTodoResult()

    data object SignedOut : AddTodoResult()

    data class Failed(val error: MessageKey) : AddTodoResult()
}
