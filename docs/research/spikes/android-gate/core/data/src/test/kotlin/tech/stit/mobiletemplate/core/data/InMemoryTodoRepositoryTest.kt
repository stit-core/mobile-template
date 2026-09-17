package tech.stit.mobiletemplate.core.data

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryTodoRepositoryTest {
    @Test
    fun `add добавляет элемент в поток`() = runTest {
        val repo = InMemoryTodoRepository()
        repo.todos.test {
            assertEquals(2, awaitItem().size)
            repo.add("Новая задача")
            val after = awaitItem()
            assertEquals(3, after.size)
            assertEquals("Новая задача", after.last().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle переключает флаг done`() = runTest {
        val repo = InMemoryTodoRepository()
        repo.todos.test {
            val first = awaitItem().first()
            assertTrue(!first.done)
            repo.toggle(first.id)
            assertTrue(awaitItem().first { it.id == first.id }.done)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
