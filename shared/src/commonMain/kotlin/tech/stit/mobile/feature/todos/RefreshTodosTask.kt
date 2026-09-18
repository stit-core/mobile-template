package tech.stit.mobile.feature.todos

import tech.stit.mobile.core.background.BackgroundCondition
import tech.stit.mobile.core.background.BackgroundRejection
import tech.stit.mobile.core.background.BackgroundResult
import tech.stit.mobile.core.background.BackgroundRunner
import tech.stit.mobile.core.background.BackgroundTask
import tech.stit.mobile.core.background.BackgroundTiming
import tech.stit.mobile.core.background.BackgroundTrigger
import tech.stit.mobile.core.lib.ConnectivityMonitor
import tech.stit.mobile.core.lib.attempt
import tech.stit.mobile.entity.todo.TodoRepository
import kotlin.time.Duration.Companion.hours

// Обновляет список без пользователя на экране: снимок в кеше свеж к открытию приложения и к перерисовке виджета.
internal class RefreshTodosTask(
    private val todos: TodoRepository,
    private val connectivity: ConnectivityMonitor,
) : BackgroundRunner {
    override suspend fun run(): BackgroundResult {
        if (!connectivity.online.value) return BackgroundResult.Rejected(BackgroundRejection.Offline)
        return attempt { todos.refresh() }.fold(
            onSuccess = { BackgroundResult.RefreshWidget(TodayTodosWidgetProvider.SLUG) },
            onFailure = { BackgroundResult.Rejected(BackgroundRejection.Failed) },
        )
    }

    companion object {
        const val ID = "refresh-todos"

        private val PERIOD = 6.hours

        val TASK = BackgroundTask(
            id = ID,
            trigger = BackgroundTrigger.Periodic,
            timing = BackgroundTiming.Window,
            conditions = listOf(BackgroundCondition.Network),
            periodMinutes = PERIOD.inWholeMinutes,
        )
    }
}
