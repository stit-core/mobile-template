package tech.stit.mobile.android.core.platform.action

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.R
import tech.stit.mobile.android.core.platform.push.NotificationChannels
import tech.stit.mobile.android.core.platform.push.PushMessage
import tech.stit.mobile.android.core.platform.push.PushNotifier
import tech.stit.mobile.android.core.platform.widget.WidgetRefresher
import tech.stit.mobile.feature.todos.CompleteTodoResult
import tech.stit.mobile.feature.todos.TodayTodosWidgetProvider
import javax.inject.Inject
import javax.inject.Singleton

// Общий вход кнопки виджета и кнопки уведомления: решение принимает shared, здесь — только видимая пользователю реакция.
@Singleton
class CompleteTodoActionHandler @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val container: SharedContainer,
    private val widgets: WidgetRefresher,
) {
    suspend fun complete(todoId: String, notificationId: Int?): CompleteTodoResult {
        val result = container.completeTodoAction().run(todoId)
        when (result) {
            is CompleteTodoResult.Done, is CompleteTodoResult.AlreadyDone -> {
                notificationId?.let { NotificationManagerCompat.from(context).cancel(it) }
                widgets.refresh(TodayTodosWidgetProvider.SLUG)
            }

            CompleteTodoResult.SignedOut -> report(R.string.core_platform_action_complete_todo_signed_out, LOGIN_PATH)

            is CompleteTodoResult.Failed -> report(R.string.core_platform_action_complete_todo_failed, "$TODOS_PATH/$todoId")
        }
        return result
    }

    // Действие идёт без экрана, поэтому отказ виден единственным доступным способом — уведомлением со ссылкой, где чинить.
    private fun report(@StringRes body: Int, path: String) = PushNotifier.show(
        context,
        PushMessage(
            title = context.getString(R.string.core_platform_action_complete_todo_title),
            body = context.getString(body),
            deepLink = "https://${container.config.deepLinkHost}$path",
            channel = NotificationChannels.GENERAL,
            notificationId = NOTIFICATION_ID,
        ),
    )

    private companion object {
        const val LOGIN_PATH = "/login"
        const val TODOS_PATH = "/todos"

        // Отказы действия перекрывают друг друга: копить их в шторке незачем.
        const val NOTIFICATION_ID = -1
    }
}
