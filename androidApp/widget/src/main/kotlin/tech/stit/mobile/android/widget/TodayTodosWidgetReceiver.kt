package tech.stit.mobile.android.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// Точка входа виджета в ОС: экземпляр создаёт система, поэтому граф приложения достаётся entry point'ом Hilt.
class TodayTodosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTodosWidget()
}
