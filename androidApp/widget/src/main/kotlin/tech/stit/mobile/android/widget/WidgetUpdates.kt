package tech.stit.mobile.android.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.widget.WidgetRefresher
import tech.stit.mobile.feature.todos.TodayTodosWidgetProvider
import javax.inject.Inject
import javax.inject.Singleton

// on-app-use: пока приложение живо, каждое изменение задач переносится в снимок — виджет не остаётся на placeholder.
@Singleton
class WidgetUpdates @Inject internal constructor(
    private val container: SharedContainer,
    private val widgets: WidgetRefresher,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            container.todoRepository.todos.collect { widgets.refresh(TodayTodosWidgetProvider.SLUG) }
        }
    }
}
