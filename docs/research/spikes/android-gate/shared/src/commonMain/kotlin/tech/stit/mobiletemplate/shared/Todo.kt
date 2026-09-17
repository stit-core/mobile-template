package tech.stit.mobiletemplate.shared

data class SharedTodo(
    val id: String,
    val title: String,
    val done: Boolean = false,
)

/** Общий для Android и iOS use-case: сортировка списка для показа. */
class SortTodosForDisplay {
    operator fun invoke(items: List<SharedTodo>): List<SharedTodo> = items.sortedWith(compareBy({ it.done }, { it.title.lowercase() }))
}
