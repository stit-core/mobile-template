package tech.stit.mobiletemplate.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TodoTest {
    @Test
    fun `пустой заголовок отвергается`() {
        assertThrows(IllegalArgumentException::class.java) {
            Todo(TodoId("1"), " ")
        }
    }

    @Test
    fun `невыполненные идут первыми`() {
        val list =
            listOf(
                Todo(TodoId("1"), "Beta", done = true),
                Todo(TodoId("2"), "alpha", done = false),
                Todo(TodoId("3"), "Gamma", done = false),
            )
        assertEquals(listOf("alpha", "Gamma", "Beta"), list.sortedForDisplay().map { it.title })
    }
}
