package tech.stit.mobile.android.core.platform.action

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.push.NotificationChannels
import tech.stit.mobile.android.core.platform.push.PushMessage
import tech.stit.mobile.android.core.platform.push.PushNotifier
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Кнопка «Готово» в напоминании: уведомление несёт адресата ссылкой, приёмник зовёт действие и убирает уведомление.
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class NotificationActionReceiverTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var container: SharedContainer

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val manager = application.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
        NotificationChannels.create(application)
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun `напоминание несёт кнопку с интентом приёмника, остальные категории — без кнопок`() {
        PushNotifier.show(application, reminder())
        PushNotifier.show(application, reminder().copy(channel = NotificationChannels.GENERAL, notificationId = OTHER_ID))

        val shown = shadowOf(manager).allNotifications
        val action = shown.first().actions.single()
        assertEquals("Done", action.title.toString())
        val intent = shadowOf(action.actionIntent).savedIntent
        assertEquals(NotificationActionReceiver::class.java.name, intent.component?.className)
        assertEquals("https://app.demo.invalid/todos/todo-1", intent.getStringExtra("link"))
        assertEquals(REMINDER_ID, intent.getIntExtra("notification_id", 0))
        assertTrue(shown.last().actions.isNullOrEmpty())
    }

    @Test
    fun `нажатие закрывает задачу и снимает напоминание`() = runBlocking {
        container.sessionRepository.login("demo@example.com", "password")
        container.todoRepository.refresh()
        PushNotifier.show(application, reminder())

        NotificationActionReceiver().handle(application, completeIntent())

        assertTrue(container.todoRepository.todos.value.single { it.id == "todo-1" }.done)
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun `без сессии задача не трогается, а причина приходит уведомлением`() = runBlocking {
        NotificationActionReceiver().handle(application, completeIntent())

        assertTrue(container.todoRepository.todos.value.isEmpty())
        val notification = shadowOf(manager).allNotifications.single()
        assertEquals(NotificationChannels.GENERAL, notification.channelId)
        assertEquals("Task is still open", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("https://app.demo.invalid/login", shadowOf(notification.contentIntent).savedIntent.dataString)
    }

    @Test
    fun `интент без ссылки и с чужим маршрутом действие не запускает`() = runBlocking {
        container.sessionRepository.login("demo@example.com", "password")
        container.todoRepository.refresh()

        NotificationActionReceiver().handle(application, Intent(application, NotificationActionReceiver::class.java))
        NotificationActionReceiver().handle(
            application,
            NotificationActionReceiver.completeTodoIntent(application, "https://app.demo.invalid/settings", REMINDER_ID),
        )

        assertFalse(container.todoRepository.todos.value.single { it.id == "todo-1" }.done)
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun `приёмник исполняет работу вне своего вызова`() {
        runBlocking { container.sessionRepository.login("demo@example.com", "password") }

        NotificationActionReceiver().onReceive(application, completeIntent())

        waitUntil { container.todoRepository.todos.value.any { it.id == "todo-1" && it.done } }
    }

    private fun completeIntent(): Intent =
        NotificationActionReceiver.completeTodoIntent(application, "https://app.demo.invalid/todos/todo-1", REMINDER_ID)

    private fun reminder() = PushMessage(
        title = "Task reminder",
        body = "Don't forget: Прочитать docs/reference/architecture.md",
        deepLink = "https://app.demo.invalid/todos/todo-1",
        channel = NotificationChannels.TODO_REMINDER,
        notificationId = REMINDER_ID,
    )

    // Приёмник отдаёт работу корутине процесса: тест ждёт её результат, а не порядок диспетчеризации.
    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(POLL_MS)
        }
        throw AssertionError("работа приёмника не завершилась за $TIMEOUT_MS мс")
    }

    private companion object {
        const val REMINDER_ID = 42
        const val OTHER_ID = 43
        const val TIMEOUT_MS = 10_000L
        const val POLL_MS = 25L
    }
}
