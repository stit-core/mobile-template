package tech.stit.mobiletemplate.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class SortTodosForDisplayTest {
    @Test
    fun undoneFirstThenByTitle() {
        val result = SortTodosForDisplay()(
            listOf(
                SharedTodo("1", "Beta", done = true),
                SharedTodo("2", "alpha"),
                SharedTodo("3", "Gamma"),
            ),
        )
        assertEquals(listOf("alpha", "Gamma", "Beta"), result.map { it.title })
    }
}
