package tech.stit.mobile.android.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

// Кнопка строки виджета: систему интересует один вызов вне процесса приложения, решение принимает общий обработчик.
class TodayTodosWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val todoId = parameters[TODO_ID] ?: return
        context.completeTodoActionHandler().complete(todoId, notificationId = null)
    }

    companion object {
        val TODO_ID = ActionParameters.Key<String>("todo_id")
    }
}
