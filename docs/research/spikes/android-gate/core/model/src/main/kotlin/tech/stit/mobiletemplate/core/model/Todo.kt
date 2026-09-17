package tech.stit.mobiletemplate.core.model

@JvmInline
value class TodoId(
    val value: String,
)

data class Todo(
    val id: TodoId,
    val title: String,
    val done: Boolean = false,
) {
    init {
        require(title.isNotBlank()) { "title не может быть пустым" }
    }
}

/** Доменное правило: показываем невыполненные сверху, внутри группы — по заголовку. */
fun List<Todo>.sortedForDisplay(): List<Todo> = sortedWith(compareBy({ it.done }, { it.title.lowercase() }))

@Suppress("unused")
fun List<Todo>.pendingCount(): Int = count { !it.done }
