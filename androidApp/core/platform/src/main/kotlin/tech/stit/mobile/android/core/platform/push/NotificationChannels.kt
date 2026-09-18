package tech.stit.mobile.android.core.platform.push

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import tech.stit.mobile.android.core.platform.R

// Каналы = категории из docs/stack/push.md (id — category в нижнем регистре); создаются на старте процесса, повторный вызов безвреден.
object NotificationChannels {
    const val GENERAL = "general"
    const val TODO_REMINDER = "todo_reminder"

    fun create(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            channel(
                context,
                GENERAL,
                R.string.core_platform_channel_general_name,
                R.string.core_platform_channel_general_description,
            ),
        )
        manager.createNotificationChannel(
            channel(
                context,
                TODO_REMINDER,
                R.string.core_platform_channel_todo_reminder_name,
                R.string.core_platform_channel_todo_reminder_description,
            ),
        )
    }

    private fun channel(context: Context, id: String, name: Int, description: Int): NotificationChannelCompat =
        NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(name))
            .setDescription(context.getString(description))
            .build()
}
