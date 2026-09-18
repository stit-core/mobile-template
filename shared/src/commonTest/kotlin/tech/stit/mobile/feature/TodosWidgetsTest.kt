package tech.stit.mobile.feature

import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.core.widget.WidgetState
import tech.stit.mobile.testing.FakeClock
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.signIn
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TodosWidgetsTest {
    @Test
    fun `placeholder — снимка ещё нет, виджет ждёт первых данных`() = runTest {
        val container = testContainer()
        container.signIn()

        val snapshot = container.todayTodosWidgetProvider().snapshot()

        assertEquals(WidgetState.Placeholder, snapshot.state)
        assertTrue(snapshot.items.isEmpty())
        assertEquals("https://app.demo.invalid/todos", snapshot.tapUrl)
    }

    @Test
    fun `populated — три ближайшие задачи, незакрытые первыми`() = runTest {
        val container = testContainer()
        container.signIn()
        container.todoRepository.refresh()
        val provider = container.todayTodosWidgetProvider()

        provider.publish()
        val snapshot = provider.snapshot()

        assertEquals(WidgetState.Populated, snapshot.state)
        assertEquals(
            listOf("Проверить deep link todos/todo-3", "Прочитать docs/reference/architecture.md", "Собрать demo-сборку"),
            snapshot.items.map { it.title },
        )
        assertEquals(listOf(false, false, true), snapshot.items.map { it.done })
        assertEquals("2026-09-12T11:00:00Z", snapshot.items.first().updatedAt.toString())
    }

    @Test
    fun `empty — задач нет, виджет говорит об этом вместо пустого места`() = runTest {
        val container = testContainer(MockScenario.Empty)
        container.signIn()
        container.todoRepository.refresh()
        val provider = container.todayTodosWidgetProvider()

        provider.publish()

        assertEquals(WidgetState.Empty, provider.snapshot().state)
    }

    @Test
    fun `signed-out — снимок без задач, чужие заголовки на экране не остаются`() = runTest {
        val container = testContainer()
        container.signIn()
        container.todoRepository.refresh()
        val provider = container.todayTodosWidgetProvider()
        provider.publish()

        container.sessionStore.clear()
        provider.publish()

        assertEquals(WidgetState.SignedOut, provider.snapshot().state)
        assertTrue(provider.snapshot().items.isEmpty())
    }

    @Test
    fun `stale — снимок старше часа и обновить его нечем`() = runTest {
        val platform = FakePlatformDeps()
        val clock = FakeClock()
        val container = testContainer(platform = platform, clock = clock)
        container.signIn()
        container.todoRepository.refresh()
        val provider = container.todayTodosWidgetProvider()
        provider.publish()

        platform.connectivity.online.value = false
        clock.current += 2.hours
        val snapshot = provider.snapshot()

        assertEquals(WidgetState.Stale, snapshot.state)
        assertEquals(3, snapshot.items.size)
    }

    @Test
    fun `свежий снимок офлайн и старый при сети остаются populated`() = runTest {
        val platform = FakePlatformDeps()
        val clock = FakeClock()
        val container = testContainer(platform = platform, clock = clock)
        container.signIn()
        container.todoRepository.refresh()
        val provider = container.todayTodosWidgetProvider()
        provider.publish()

        platform.connectivity.online.value = false
        clock.current += 30.minutes
        assertEquals(WidgetState.Populated, provider.snapshot().state)

        platform.connectivity.online.value = true
        clock.current += 2.hours
        assertEquals(WidgetState.Populated, provider.snapshot().state)
    }

    @Test
    fun `в новом процессе снимок собирается из кеша задач, а не из сети`() = runTest {
        val platform = FakePlatformDeps()
        val fileSystem = FakeFileSystem()
        val app = testContainer(platform = platform, fileSystem = fileSystem)
        app.signIn()
        app.todoRepository.refresh()

        val widgetProcess = testContainer(platform = platform, fileSystem = fileSystem)
        widgetProcess.signIn()
        platform.connectivity.online.value = false
        val snapshot = widgetProcess.todayTodosWidgetProvider().publish()

        assertEquals(WidgetState.Populated, snapshot.state)
        assertEquals(3, snapshot.items.size)
    }
}
