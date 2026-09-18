package tech.stit.mobile.feature

import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.background.BackgroundCondition
import tech.stit.mobile.core.background.BackgroundRejection
import tech.stit.mobile.core.background.BackgroundResult
import tech.stit.mobile.core.background.BackgroundTiming
import tech.stit.mobile.core.background.BackgroundTrigger
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.testing.FakeClock
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.signIn
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodosTasksTest {
    @Test
    fun `объявленная задача — периодическое обновление при сети, платформа получает её описание`() = runTest {
        val platform = FakePlatformDeps()
        val container = testContainer(platform = platform)
        container.backgroundTasks.forEach(platform.background::schedule)

        val task = platform.background.scheduled.single()
        assertEquals("refresh-todos", task.id)
        assertEquals(BackgroundTrigger.Periodic, task.trigger)
        assertEquals(BackgroundTiming.Window, task.timing)
        assertEquals(listOf(BackgroundCondition.Network), task.conditions)
        assertEquals(360, task.periodMinutes)
        assertNull(task.at)

        platform.background.cancel(task.id)
        assertEquals(listOf("refresh-todos"), platform.background.cancelled)
    }

    @Test
    fun `обновление списка наполняет репозиторий и просит перерисовать виджет`() = runTest {
        val container = testContainer()
        container.signIn()
        val runner = checkNotNull(container.backgroundRunner("refresh-todos"))

        assertEquals(BackgroundResult.RefreshWidget("today-todos"), runner.run())
        assertEquals(3, container.todoRepository.todos.value.size)
    }

    @Test
    fun `отказ API — Rejected и пустой список, данные обновятся при открытии приложения`() = runTest {
        val container = testContainer(MockScenario.Error)
        val runner = checkNotNull(container.backgroundRunner("refresh-todos"))

        assertEquals(BackgroundResult.Rejected(BackgroundRejection.Failed), runner.run())
        assertTrue(container.todoRepository.todos.value.isEmpty())
    }

    @Test
    fun `без сети обновление не идёт в API`() = runTest {
        val platform = FakePlatformDeps()
        platform.connectivity.online.value = false
        val container = testContainer(platform = platform)
        val runner = checkNotNull(container.backgroundRunner("refresh-todos"))

        assertEquals(BackgroundResult.Rejected(BackgroundRejection.Offline), runner.run())
        assertTrue(container.todoRepository.todos.value.isEmpty())
    }

    @Test
    fun `напоминание строит локальное уведомление с deep link на задачу`() = runTest {
        val container = testContainer()
        container.signIn()
        container.todoRepository.refresh()
        val task = container.todoReminderTask("todo-1", FakeClock().current)
        assertEquals(BackgroundTrigger.Time, task.trigger)
        assertEquals(BackgroundTiming.Exact, task.timing)

        val result = checkNotNull(container.backgroundRunner(task.id)).run()

        val notify = assertIs<BackgroundResult.Notify>(result)
        assertEquals("TODO_REMINDER", notify.category)
        assertEquals("push.todo.reminder.title", notify.titleKey)
        assertEquals("push.todo.reminder.body", notify.bodyKey)
        assertEquals(listOf("Прочитать docs/reference/architecture.md"), notify.args)
        assertEquals("https://app.demo.invalid/todos/todo-1", notify.deepLink)
        assertEquals("todo-reminder:todo-1", notify.notificationId)
    }

    @Test
    fun `напоминание без сети берёт задачу из кеша`() = runTest {
        val platform = FakePlatformDeps()
        val container = testContainer(platform = platform)
        container.signIn()
        container.todoRepository.refresh()
        platform.connectivity.online.value = false
        val reminder = checkNotNull(container.backgroundRunner("todo-reminder:todo-1"))

        assertIs<BackgroundResult.Notify>(reminder.run())
    }

    @Test
    fun `напоминание о закрытой и о неизвестной задаче не показывается`() = runTest {
        val container = testContainer()
        container.signIn()
        container.todoRepository.refresh()

        val done = checkNotNull(container.backgroundRunner("todo-reminder:todo-2"))
        assertEquals(BackgroundResult.Rejected(BackgroundRejection.NothingToDo), done.run())

        val missing = checkNotNull(container.backgroundRunner("todo-reminder:missing"))
        assertEquals(BackgroundResult.Rejected(BackgroundRejection.NothingToDo), missing.run())
    }

    @Test
    fun `неизвестный id задачи раннера не даёт`() = runTest {
        assertNull(testContainer().backgroundRunner("nope"))
    }
}
