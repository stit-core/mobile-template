package tech.stit.mobile.android.widget

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.action.CompleteTodoActionHandler
import tech.stit.mobile.android.core.platform.widget.WidgetRefresher
import tech.stit.mobile.feature.todos.TodayTodosWidgetSnapshot

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun sharedContainer(): SharedContainer

    fun completeTodoActionHandler(): CompleteTodoActionHandler
}

// Порт перерисовки виджетов связывается здесь: core:platform про Glance не знает, а зовёт обновление по slug.
@Module
@InstallIn(SingletonComponent::class)
internal interface WidgetModule {
    @Binds
    fun widgetRefresher(refresher: GlanceWidgetRefresher): WidgetRefresher
}

internal suspend fun Context.todayTodosSnapshot(): TodayTodosWidgetSnapshot =
    EntryPointAccessors.fromApplication(this, WidgetEntryPoint::class.java)
        .sharedContainer()
        .todayTodosWidgetProvider()
        .snapshot()

internal fun Context.completeTodoActionHandler(): CompleteTodoActionHandler =
    EntryPointAccessors.fromApplication(this, WidgetEntryPoint::class.java).completeTodoActionHandler()
