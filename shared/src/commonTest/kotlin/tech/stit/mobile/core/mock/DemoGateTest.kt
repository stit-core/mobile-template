package tech.stit.mobile.core.mock

import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.background.BackgroundResult
import tech.stit.mobile.core.lib.SettingsKeys
import tech.stit.mobile.core.widget.WidgetState
import tech.stit.mobile.feature.shell.ShellIntent
import tech.stit.mobile.feature.todos.CompleteTodoResult
import tech.stit.mobile.feature.todos.RefreshTodosTask
import tech.stit.mobile.feature.todos.TodayTodosWidgetProvider
import tech.stit.mobile.testing.FakeBackend
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.prodContainer
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Runtime-переключатель demo в обычной сборке: кто решает о доступности входа, куда уходит транспорт и чего
// demo-сессия не имеет права коснуться на реальной стороне.
class DemoGateTest {
    @Test
    fun `без флага demo_login вход недоступен и не включается`() = runTest {
        val backend = FakeBackend(StandardTestDispatcher(testScheduler)).apply { demoLogin = false }
        val container = prodContainer(backend = backend)
        val gate = requireNotNull(container.demoGate)
        container.requirements.fetch()

        assertFalse(gate.available.first())
        gate.enter()
        assertFalse(gate.active.value)
        assertSame(backend.engine, container.apiClient.http.engine)
    }

    @Test
    fun `недоступные requirements — безопасный дефолт без входа в demo`() = runTest {
        val backend = FakeBackend(StandardTestDispatcher(testScheduler)).apply { hasRequirements = false }
        val container = prodContainer(backend = backend)
        val gate = requireNotNull(container.demoGate)
        container.requirements.fetch()

        assertFalse(gate.available.first())
        gate.enter()
        assertFalse(gate.active.value)
    }

    @Test
    fun `в demo-сборке переключать нечего — гейта нет, панель на месте`() = runTest {
        val container = testContainer()
        assertNull(container.demoGate)
        assertTrue(container.apiClient.http.engine is MockEngine)
        assertTrue(container.demoPanelPresenter().state.value.available)
    }

    @Test
    fun `вход в demo при живой реальной сессии не трогает её токены`() = runTest {
        val platform = FakePlatformDeps()
        platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY] = "real-refresh-1"
        val backend = FakeBackend(StandardTestDispatcher(testScheduler))
        val container = prodContainer(platform = platform, backend = backend)
        assertEquals(AuthState.SignedIn, container.sessionStore.restore())
        container.requirements.fetch()
        assertEquals("Настоящий Пользователь", container.userRepository.refresh().name)
        val realCalls = backend.paths.size

        requireNotNull(container.demoGate).enter()

        assertEquals(AuthState.SignedIn, container.sessionStore.authState.value)
        assertEquals("Демо Пользователь", container.userRepository.refresh().name)
        assertEquals(3, container.todoRepository.refresh().size)
        // Реальная сторона нетронута: ни одного запроса к бэкенду и ни одной записи в её хранилищах.
        assertEquals(realCalls, backend.paths.size)
        assertEquals("real-refresh-2", platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY])
        assertNull(platform.settings.values[SettingsKeys.MOCK_SCENARIO])
    }

    @Test
    fun `выход из demo возвращает обычное состояние — экран входа и реальные ссылки`() = runTest {
        val platform = FakePlatformDeps()
        val backend = FakeBackend(StandardTestDispatcher(testScheduler))
        val container = prodContainer(platform = platform, backend = backend)
        val gate = requireNotNull(container.demoGate)
        container.requirements.fetch()

        gate.enter()
        assertTrue(gate.active.value)
        assertEquals("https://app.example.com/privacy", container.requirements.current.value.links.privacyPolicy)

        gate.leave()

        assertFalse(gate.active.value)
        assertEquals(AuthState.SignedOut, container.sessionStore.authState.value)
        assertSame(backend.engine, container.apiClient.http.engine)
        assertEquals("https://real.example.com/privacy", container.requirements.current.value.links.privacyPolicy)
        assertNull(platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY])
        assertNull(platform.settings.values[SettingsKeys.DEMO_ACTIVE])
    }

    @Test
    fun `флаг выключили посреди demo — сессия доживает, следующий вход закрыт`() = runTest {
        val backend = FakeBackend(StandardTestDispatcher(testScheduler))
        val container = prodContainer(backend = backend)
        val gate = requireNotNull(container.demoGate)
        container.requirements.fetch()
        gate.enter()

        backend.demoLogin = false
        assertTrue(gate.active.value)
        assertEquals(3, container.todoRepository.refresh().size)

        gate.leave()
        assertFalse(gate.available.first())
        gate.enter()
        assertFalse(gate.active.value)
    }

    // Поверхности вне экранов знают только SharedContainer, поэтому в demo они автоматически идут в MockTransport:
    // фоновая задача, снимок виджета и системное действие работают на фикстурах и в реальный бэкенд не стучатся.
    @Test
    fun `вне экранов demo идёт на фикстуры — задача, снимок виджета, действие`() = runTest {
        val backend = FakeBackend(StandardTestDispatcher(testScheduler))
        val container = prodContainer(backend = backend)
        container.requirements.fetch()
        requireNotNull(container.demoGate).enter()
        val realCalls = backend.paths.size

        val task = requireNotNull(container.backgroundRunner(RefreshTodosTask.ID))
        assertEquals(BackgroundResult.RefreshWidget(TodayTodosWidgetProvider.SLUG), task.run())

        val snapshot = container.todayTodosWidgetProvider().publish()
        assertEquals(WidgetState.Populated, snapshot.state)
        assertEquals("Проверить deep link todos/todo-3", snapshot.items.first().title)

        assertTrue(container.completeTodoAction().run("todo-1") is CompleteTodoResult.Done)
        assertEquals(realCalls, backend.paths.size)
    }

    @Test
    fun `холодный старт после demo — обычный режим без следов фикстур`() = runTest {
        val platform = FakePlatformDeps()
        platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY] = "real-refresh-1"
        val fileSystem = FakeFileSystem()
        val first = prodContainer(platform = platform, fileSystem = fileSystem)
        first.requirements.fetch()
        requireNotNull(first.demoGate).enter()
        assertEquals(3, first.todoRepository.refresh().size)

        // Процесс убит посреди demo: новый контейнер поднимается на тех же хранилищах платформы и том же диске.
        val backend = FakeBackend(StandardTestDispatcher(testScheduler))
        val restarted = prodContainer(platform = platform, backend = backend, fileSystem = fileSystem)
        assertFalse(requireNotNull(restarted.demoGate).active.value)
        assertSame(backend.engine, restarted.apiClient.http.engine)

        val shell = restarted.shellPresenter()
        shell.dispatch(ShellIntent.Start)
        advanceUntilIdle()

        assertFalse(shell.state.value.demo)
        assertEquals(AuthState.SignedIn, shell.state.value.auth)
        assertNull(platform.settings.values[SettingsKeys.DEMO_ACTIVE])
        // Снимки фикстур с диска ушли: обычный режим стартует пустым и наполняется из настоящего бэкенда.
        assertTrue(restarted.todoRepository.todos.value.isEmpty())
        assertEquals(listOf("Настоящая задача"), restarted.todoRepository.refresh().map { it.title })
        shell.close()
    }
}
