package tech.stit.mobile.android.core.platform.push

import android.content.Context
import tech.stit.mobile.android.core.platform.R

// Data-first payload (07 §6): title_key/body_key + args → строки клиента; title/body — запасной текст; deep_link — https из deep-links.md.
data class PushMessage(
    val title: String?,
    val body: String?,
    val deepLink: String?,
    val channel: String,
    val notificationId: Int,
) {
    companion object {
        fun parse(data: Map<String, String>, context: Context): PushMessage {
            val args: Array<String> = data["args"]?.split('|')?.toTypedArray() ?: emptyArray()
            val deepLink = data["deep_link"]
            return PushMessage(
                title = data["title_key"]?.let { context.text(it, args) } ?: data["title"],
                body = data["body_key"]?.let { context.text(it, args) } ?: data["body"],
                deepLink = deepLink,
                channel = data["category"]?.lowercase() ?: NotificationChannels.GENERAL,
                // Один notification_id через два транспорта — одно уведомление (дедуп по id, 07 §6).
                notificationId = (data["notification_id"] ?: deepLink ?: data["title"].orEmpty()).hashCode(),
            )
        }

        // Resources.getString принимает только vararg — спред здесь единственный способ подставить аргументы.
        @Suppress("SpreadOperator")
        private fun Context.text(key: String, args: Array<String>): String? = TEXTS[key]?.let { getString(it, *args) }

        // Реестр ключей payload → ресурсы; новая категория — строка в docs/stack/push.md и здесь.
        private val TEXTS: Map<String, Int> = mapOf(
            "push.todo.reminder.title" to R.string.core_platform_push_todo_reminder_title,
            "push.todo.reminder.body" to R.string.core_platform_push_todo_reminder_body,
        )
    }
}
