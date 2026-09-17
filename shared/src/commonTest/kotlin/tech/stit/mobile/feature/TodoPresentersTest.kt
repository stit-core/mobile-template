package tech.stit.mobile.feature

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.feature.todos.TodoDetailIntent
import tech.stit.mobile.feature.todos.TodoListIntent
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.signIn
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodoPresentersTest {
    @Test
    fun listLoadsCreatesTogglesAndDeletes() = runTest {
        val container = testContainer()
        container.signIn()
        val presenter = container.todoListPresenter()
        presenter.dispatch(TodoListIntent.Start)
        advanceUntilIdle()
        assertEquals(3, presenter.state.value.todos.size)
        assertFalse(presenter.state.value.loading)

        presenter.dispatch(TodoListIntent.TitleChanged("  Новая задача "))
        assertTrue(presenter.state.value.canCreate)
        presenter.dispatch(TodoListIntent.Create)
        advanceUntilIdle()
        assertEquals("", presenter.state.value.newTitle)
        assertEquals("Новая задача", presenter.state.value.todos.first().title)

        val id = presenter.state.value.todos.first().id
        presenter.dispatch(TodoListIntent.Toggle(id))
        advanceUntilIdle()
        assertTrue(presenter.state.value.todos.first { it.id == id }.done)
        presenter.dispatch(TodoListIntent.Delete(id))
        advanceUntilIdle()
        assertEquals(3, presenter.state.value.todos.size)
        presenter.close()
    }

    @Test
    fun emptyScenarioShowsEmptyListAndErrorScenarioShowsError() = runTest {
        val empty = testContainer(MockScenario.Empty)
        empty.signIn()
        val emptyPresenter = empty.todoListPresenter()
        emptyPresenter.dispatch(TodoListIntent.Start)
        advanceUntilIdle()
        assertTrue(emptyPresenter.state.value.todos.isEmpty())
        emptyPresenter.close()

        val failing = testContainer(MockScenario.Error).todoListPresenter()
        failing.dispatch(TodoListIntent.Start)
        advanceUntilIdle()
        assertEquals(MessageKey("error.internal"), failing.state.value.error)
        failing.close()
    }

    @Test
    fun offlineIsReadOnly() = runTest {
        val platform = FakePlatformDeps()
        val container = testContainer(platform = platform)
        container.signIn()
        val presenter = container.todoListPresenter()
        presenter.dispatch(TodoListIntent.Start)
        advanceUntilIdle()
        platform.connectivity.online.value = false
        advanceUntilIdle()
        assertTrue(presenter.state.value.offline)
        presenter.dispatch(TodoListIntent.TitleChanged("x"))
        assertFalse(presenter.state.value.canCreate)
        presenter.dispatch(TodoListIntent.Delete("todo-1"))
        advanceUntilIdle()
        assertEquals(3, presenter.state.value.todos.size)
        presenter.close()
    }

    @Test
    fun detailLoadsTogglesAndDeletes() = runTest {
        val container = testContainer()
        container.signIn()
        val presenter = container.todoDetailPresenter("todo-1")
        presenter.dispatch(TodoDetailIntent.Start)
        advanceUntilIdle()
        assertEquals("Прочитать docs/reference/architecture.md", presenter.state.value.todo?.title)
        presenter.dispatch(TodoDetailIntent.Toggle)
        advanceUntilIdle()
        assertEquals(true, presenter.state.value.todo?.done)
        presenter.dispatch(TodoDetailIntent.Delete)
        advanceUntilIdle()
        assertTrue(presenter.state.value.deleted)
        presenter.close()
    }

    @Test
    fun detailOfUnknownTodoReportsNotFound() = runTest {
        val container = testContainer()
        container.signIn()
        val presenter = container.todoDetailPresenter("missing")
        presenter.dispatch(TodoDetailIntent.Start)
        advanceUntilIdle()
        assertEquals(MessageKey("error.not_found"), presenter.state.value.error)
        assertFalse(presenter.state.value.loading)
        presenter.close()
    }
}
