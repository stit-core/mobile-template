package tech.stit.mobile.feature.todos

import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.entity.todo.Todo

data class TodoListState(
    val todos: List<Todo> = emptyList(),
    val loading: Boolean = false,
    val error: MessageKey? = null,
    // Офлайн — только чтение снимка: мутации без сети не ставятся в очередь (08 §2.7).
    val offline: Boolean = false,
    val newTitle: String = "",
) {
    val canCreate: Boolean get() = newTitle.isNotBlank() && !offline
}
