package tech.stit.mobile.android.core.platform.push

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import tech.stit.mobile.android.core.platform.R

// Каналы = категории из docs/stack/push.md; создаются на старте процесса, повторный вызов безвреден.
object NotificationChannels {
    const val GENERAL = "general"

    fun create(context: Context) {
        val general = NotificationChannelCompat.Builder(GENERAL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.core_platform_channel_general_name))
            .setDescription(context.getString(R.string.core_platform_channel_general_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(general)
    }
}
