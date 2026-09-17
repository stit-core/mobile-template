package tech.stit.mobile.android.core.platform.push

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class PushMessageTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val notificationManager = application.getSystemService(NotificationManager::class.java)

    @Test
    fun `ключи payload превращаются в строки клиента с аргументами`() {
        val data = mapOf(
            "title_key" to "push.todo.reminder.title",
            "body_key" to "push.todo.reminder.body",
            "args" to "Собрать demo-сборку",
            "deep_link" to "https://app.demo.invalid/todos/todo-2",
            "category" to "GENERAL",
            "notification_id" to "n-42",
        )
        val message = PushMessage.parse(data, application)
        assertEquals("Task reminder", message.title)
        assertEquals("Don't forget: Собрать demo-сборку", message.body)
        assertEquals("https://app.demo.invalid/todos/todo-2", message.deepLink)
        assertEquals(NotificationChannels.GENERAL, message.channel)
        assertEquals("n-42".hashCode(), message.notificationId)
    }

    @Test
    fun `без ключей берётся готовый текст, неизвестный ключ — тоже запасной текст`() {
        val message = PushMessage.parse(mapOf("title" to "Привет", "body" to "Текст", "title_key" to "push.unknown"), application)
        assertEquals("Привет", message.title)
        assertEquals("Текст", message.body)
        assertNull(message.deepLink)
    }

    @Test
    fun `уведомление показывается с разрешением и ведёт по deep link`() {
        NotificationChannels.create(application)
        assertNotNull(notificationManager.getNotificationChannel(NotificationChannels.GENERAL))
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val message = PushMessage.parse(mapOf("title" to "Привет", "deep_link" to "stit://todos/todo-1"), application)

        PushNotifier.show(application, message)
        val notification = shadowOf(notificationManager).allNotifications.single()
        assertEquals(NotificationChannels.GENERAL, notification.channelId)
        assertNotNull(notification.contentIntent)
        assertEquals("stit://todos/todo-1", shadowOf(notification.contentIntent).savedIntent.dataString)
    }

    @Test
    fun `без разрешения уведомление не показывается`() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        PushNotifier.show(application, PushMessage.parse(mapOf("title" to "Привет"), application))
        assertEquals(0, shadowOf(notificationManager).allNotifications.size)
    }
}
