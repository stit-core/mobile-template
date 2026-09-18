package tech.stit.mobile.android.core.platform.push

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import tech.stit.mobile.android.core.platform.R
import tech.stit.mobile.android.core.platform.action.NotificationActionReceiver

// Тап по уведомлению открывает deep link тем же intent-filter, что и ссылка из веба: роутер один (rule deep-links-and-push).
object PushNotifier {
    fun show(context: Context, message: PushMessage) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val notification = NotificationCompat.Builder(context, message.channel)
            .setSmallIcon(R.drawable.core_platform_ic_notification)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setAutoCancel(true)
            .setContentIntent(message.deepLink?.let { deepLinkIntent(context, it) })
            .apply { actions(context, message).forEach(::addAction) }
            .build()
        NotificationManagerCompat.from(context).notify(message.notificationId, notification)
    }

    // Кнопки категории (system-actions канала notification:<slug>): у напоминания о задаче — «Готово».
    private fun actions(context: Context, message: PushMessage): List<NotificationCompat.Action> =
        if (message.channel == NotificationChannels.TODO_REMINDER && message.deepLink != null) {
            listOf(NotificationActionReceiver.completeTodoButton(context, message.deepLink, message.notificationId))
        } else {
            emptyList()
        }

    private fun deepLinkIntent(context: Context, deepLink: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, deepLink.toUri()).setPackage(context.packageName)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
