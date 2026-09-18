package tech.stit.mobile.android.core.platform.action

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.R
import tech.stit.mobile.core.deeplink.Route

// Кнопка в уведомлении: приёмник достаёт адресата из той же ссылки, по которой открывается приложение, и зовёт обработчик.
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext
        // Приёмник живёт миллисекунды: система держит процесс живым до finish(), пока действие ходит в сеть.
        // Вне доставки системой (прямой вызов) держать нечего — goAsync() отдаёт null.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handle(application, intent)
            } finally {
                pending?.finish()
            }
        }
    }

    internal suspend fun handle(context: Context, intent: Intent) {
        val graph = EntryPointAccessors.fromApplication(context, ActionEntryPoint::class.java)
        val link = intent.getStringExtra(EXTRA_LINK) ?: return
        val todoId = (graph.sharedContainer().deepLinkParser.parse(link) as? Route.TodoDetail)?.id ?: return
        graph.completeTodoActionHandler().complete(todoId, intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
    }

    companion object {
        private const val EXTRA_LINK = "link"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"

        // Кнопка и её приёмник объявляются вместе: адресат кнопки — ссылка уведомления, а не отдельный идентификатор.
        fun completeTodoButton(context: Context, deepLink: String, notificationId: Int): NotificationCompat.Action =
            NotificationCompat.Action.Builder(
                R.drawable.core_platform_ic_notification,
                context.getString(R.string.core_platform_action_complete_todo_done),
                PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    completeTodoIntent(context, deepLink, notificationId),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            ).build()

        internal fun completeTodoIntent(context: Context, deepLink: String, notificationId: Int): Intent =
            Intent(context, NotificationActionReceiver::class.java)
                .putExtra(EXTRA_LINK, deepLink)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ActionEntryPoint {
    fun sharedContainer(): SharedContainer

    fun completeTodoActionHandler(): CompleteTodoActionHandler
}
