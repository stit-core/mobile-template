package tech.stit.mobile.android.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.widget.WidgetRefresher
import tech.stit.mobile.feature.todos.TodayTodosWidgetProvider
import javax.inject.Inject
import javax.inject.Singleton

// Снимок пишет shared, перерисовку заказывает Glance: обе половины обновления виджета живут здесь.
@Singleton
internal class GlanceWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val container: SharedContainer,
) : WidgetRefresher {
    override suspend fun refresh(slug: String) {
        if (slug != TodayTodosWidgetProvider.SLUG) return
        container.todayTodosWidgetProvider().publish()
        TodayTodosWidget().updateAll(context)
    }
}
