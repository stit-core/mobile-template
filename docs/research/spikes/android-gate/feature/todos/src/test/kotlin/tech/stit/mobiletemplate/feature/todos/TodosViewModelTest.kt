package tech.stit.mobiletemplate.feature.todos

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.stit.mobiletemplate.core.model.Todo
import tech.stit.mobiletemplate.core.model.TodoId
import tech.stit.mobiletemplate.core.testing.FakeTodoRepository
import tech.stit.mobiletemplate.core.testing.MainDispatcherRule

class TodosViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `начальное состояние — Loading`() = runTest {
        val vm = TodosViewModel(FakeTodoRepository())
        assertEquals(TodosUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `поток репозитория превращается в Ready и сортируется`() = runTest {
        val repo =
            FakeTodoRepository(
                listOf(
                    Todo(TodoId("1"), "Beta", done = true),
                    Todo(TodoId("2"), "alpha"),
                ),
            )
        val vm = TodosViewModel(repo)
        vm.uiState.test {
            assertEquals(TodosUiState.Loading, awaitItem())
            val ready = awaitItem() as TodosUiState.Ready
            assertEquals(listOf("alpha", "Beta"), ready.items.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToggle меняет состояние элемента`() = runTest {
        val repo = FakeTodoRepository(listOf(Todo(TodoId("1"), "alpha")))
        val vm = TodosViewModel(repo)
        vm.uiState.test {
            awaitItem()
            awaitItem()
            vm.onToggle(TodoId("1"))
            val ready = awaitItem() as TodosUiState.Ready
            assertTrue(ready.items.single().done)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
