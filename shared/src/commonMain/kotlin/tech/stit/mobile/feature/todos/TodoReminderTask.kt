package tech.stit.mobile.feature.todos

import tech.stit.mobile.core.background.BackgroundRejection
import tech.stit.mobile.core.background.BackgroundResult
import tech.stit.mobile.core.background.BackgroundRunner
import tech.stit.mobile.core.background.BackgroundTask
import tech.stit.mobile.core.background.BackgroundTiming
import tech.stit.mobile.core.background.BackgroundTrigger
import tech.stit.mobile.entity.todo.TodoRepository
import kotlin.time.Instant

// Напоминает о задаче к обещанному моменту; текст берётся из снимка кеша, чтобы напоминание дошло и без сети.
internal class TodoReminderTask(
    private val todoId: String,
    private val todos: TodoRepository,
    private val deepLinkHost: String,
) : BackgroundRunner {
    override suspend fun run(): BackgroundResult {
        if (todos.todos.value.isEmpty()) todos.restore()
        val todo = todos.todos.value.firstOrNull { it.id == todoId && !it.done }
            ?: return BackgroundResult.Rejected(BackgroundRejection.NothingToDo)
        return BackgroundResult.Notify(
            category = CATEGORY,
            titleKey = TITLE_KEY,
            bodyKey = BODY_KEY,
            args = listOf(todo.title),
            deepLink = "https://$deepLinkHost/todos/${todo.id}",
            notificationId = "$ID_PREFIX${todo.id}",
        )
    }

    companion object {
        const val SLUG = "todo-reminder"

        // Напоминание живёт на конкретную задачу: её id — часть ключа планировщика, раннер берёт его оттуда.
        const val ID_PREFIX = "$SLUG:"
        const val CATEGORY = "TODO_REMINDER"
        const val TITLE_KEY = "push.todo.reminder.title"
        const val BODY_KEY = "push.todo.reminder.body"

        fun task(todoId: String, at: Instant): BackgroundTask = BackgroundTask(
            id = "$ID_PREFIX$todoId",
            trigger = BackgroundTrigger.Time,
            timing = BackgroundTiming.Exact,
            at = at,
        )
    }
}
