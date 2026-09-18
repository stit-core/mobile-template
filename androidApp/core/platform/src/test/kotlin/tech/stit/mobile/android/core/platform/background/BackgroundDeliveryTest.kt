package tech.stit.mobile.android.core.platform.background

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import tech.stit.mobile.android.core.platform.push.NotificationChannels
import tech.stit.mobile.android.core.platform.widget.WidgetRefresher
import tech.stit.mobile.core.background.BackgroundRejection
import tech.stit.mobile.core.background.BackgroundResult
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class BackgroundDeliveryTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val notificationManager = application.getSystemService(NotificationManager::class.java)
    private val widgets = RecordingWidgetRefresher()

    @Test
    fun `Notify показывает уведомление в своём канале и ведёт на задачу`() {
        NotificationChannels.create(application)
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val result = deliver(reminder())

        assertEquals(ListenableWorker.Result.success(), result)
        val notification = shadowOf(notificationManager).allNotifications.single()
        assertEquals(NotificationChannels.TODO_REMINDER, notification.channelId)
        assertNotNull(notification.contentIntent)
        assertEquals(
            "https://app.demo.invalid/todos/todo-1",
            shadowOf(notification.contentIntent).savedIntent.dataString,
        )
    }

    @Test
    fun `без разрешения на уведомления работа считается выполненной`() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertEquals(ListenableWorker.Result.success(), deliver(reminder()))
        assertEquals(0, shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun `свежие данные и виджет — успех, ошибка — повтор, отказ по условиям — успех`() {
        assertEquals(ListenableWorker.Result.success(), deliver(BackgroundResult.FreshData))
        assertEquals(ListenableWorker.Result.success(), deliver(BackgroundResult.RefreshWidget("today-todos")))
        assertEquals(
            ListenableWorker.Result.retry(),
            deliver(BackgroundResult.Rejected(BackgroundRejection.Failed)),
        )
        assertEquals(
            ListenableWorker.Result.success(),
            deliver(BackgroundResult.Rejected(BackgroundRejection.Offline)),
        )
        assertEquals(listOf("today-todos"), widgets.slugs)
    }

    private fun deliver(result: BackgroundResult): ListenableWorker.Result = runBlocking { result.deliver(application, widgets) }

    private class RecordingWidgetRefresher : WidgetRefresher {
        val slugs = mutableListOf<String>()

        override suspend fun refresh(slug: String) {
            slugs += slug
        }
    }

    private fun reminder() = BackgroundResult.Notify(
        category = "TODO_REMINDER",
        titleKey = "push.todo.reminder.title",
        bodyKey = "push.todo.reminder.body",
        args = listOf("Прочитать docs/reference/architecture.md"),
        deepLink = "https://app.demo.invalid/todos/todo-1",
        notificationId = "todo-reminder:todo-1",
    )
}
