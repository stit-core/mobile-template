package tech.stit.mobile.feature

import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.core.widget.WidgetState
import tech.stit.mobile.feature.todos.AddTodoResult
import tech.stit.mobile.feature.todos.CompleteTodoResult
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.signIn
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodosActionsTest {
    @Test
    fun `отметка задачи закрывает её и обновляет снимок виджета`() = runTest {
        val container = testContainer()
        container.signIn()
        container.todoRepository.refresh()

        val result = container.completeTodoAction().run("todo-1")

        assertEquals(CompleteTodoResult.Done("Прочитать docs/reference/architecture.md"), result)
        assertTrue(container.todoRepository.todos.value.single { it.id == "todo-1" }.done)
        val snapshot = container.todayTodosWidgetProvider().snapshot()
        assertEquals(WidgetState.Populated, snapshot.state)
        assertTrue(snapshot.items.single { it.id == "todo-1" }.done)
    }

    @Test
    fun `повторная отметка не открывает задачу заново`() = runTest {
        val container = testContainer()
        container.signIn()
        container.todoRepository.refresh()

        val result = container.completeTodoAction().run("todo-2")

        assertEquals(CompleteTodoResult.AlreadyDone("Собрать demo-сборку"), result)
        assertTrue(container.todoRepository.todos.value.single { it.id == "todo-2" }.done)
    }

    @Test
    fun `без сессии отметка просит войти, а не входит сама`() = runTest {
        val container = testContainer()

        assertEquals(CompleteTodoResult.SignedOut, container.completeTodoAction().run("todo-1"))
        assertTrue(container.todoRepository.todos.value.isEmpty())
    }

    @Test
    fun `отказ API при отметке — Failed с ключом ошибки, задача остаётся открытой`() = runTest {
        val container = testContainer()
        container.signIn()
        container.todoRepository.refresh()
        checkNotNull(container.mockTransport).reset(MockScenario.Error)

        val result = container.completeTodoAction().run("todo-1")

        assertEquals(CompleteTodoResult.Failed(MessageKey("error.internal")), result)
        assertFalse(container.todoRepository.todos.value.single { it.id == "todo-1" }.done)
    }

    @Test
    fun `офлайн отметка в сеть не идёт и сообщает о связи`() = runTest {
        val platform = FakePlatformDeps()
        val container = testContainer(platform = platform)
        container.signIn()
        container.todoRepository.refresh()
        platform.connectivity.online.value = false

        val result = container.completeTodoAction().run("todo-1")

        assertEquals(CompleteTodoResult.Failed(MessageKey.NetworkError), result)
        assertFalse(container.todoRepository.todos.value.single { it.id == "todo-1" }.done)
    }

    @Test
    fun `продиктованная задача попадает в список и в снимок виджета`() = runTest {
        val container = testContainer()
        container.signIn()
        container.todoRepository.refresh()

        val result = container.addTodoAction().run("  Купить молоко  ")

        assertEquals(AddTodoResult.Added("Купить молоко"), result)
        assertEquals("Купить молоко", container.todoRepository.todos.value.first().title)
        assertEquals("Купить молоко", container.todayTodosWidgetProvider().snapshot().items.first().title)
    }

    @Test
    fun `задача без текста не заводится`() = runTest {
        val container = testContainer()
        container.signIn()
        container.todoRepository.refresh()

        assertEquals(AddTodoResult.Failed(MessageKey.ValidationError), container.addTodoAction().run("   "))
        assertEquals(3, container.todoRepository.todos.value.size)
    }

    @Test
    fun `без сессии добавление просит войти`() = runTest {
        val container = testContainer()

        assertEquals(AddTodoResult.SignedOut, container.addTodoAction().run("Купить молоко"))
        assertTrue(container.todoRepository.todos.value.isEmpty())
    }

    @Test
    fun `отказ API при добавлении — Failed с ключом ошибки`() = runTest {
        val container = testContainer()
        container.signIn()
        checkNotNull(container.mockTransport).reset(MockScenario.Error)

        assertEquals(AddTodoResult.Failed(MessageKey("error.internal")), container.addTodoAction().run("Купить молоко"))
        assertTrue(container.todoRepository.todos.value.isEmpty())
    }

    @Test
    fun `офлайн добавление в сеть не идёт и сообщает о связи`() = runTest {
        val platform = FakePlatformDeps()
        val container = testContainer(platform = platform)
        container.signIn()
        platform.connectivity.online.value = false

        assertEquals(AddTodoResult.Failed(MessageKey.NetworkError), container.addTodoAction().run("Купить молоко"))
        assertTrue(container.todoRepository.todos.value.isEmpty())
    }
}
