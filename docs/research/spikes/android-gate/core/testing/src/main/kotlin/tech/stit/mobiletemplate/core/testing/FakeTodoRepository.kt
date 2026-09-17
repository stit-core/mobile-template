package tech.stit.mobiletemplate.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tech.stit.mobiletemplate.core.data.TodoRepository
import tech.stit.mobiletemplate.core.model.Todo
import tech.stit.mobiletemplate.core.model.TodoId

class FakeTodoRepository(
    initial: List<Todo> = emptyList(),
) : TodoRepository {
    private val state = MutableStateFlow(initial)

    override val todos: Flow<List<Todo>> = state.asStateFlow()

    override suspend fun add(title: String): TodoId {
        val id = TodoId("fake-${state.value.size + 1}")
        state.update { it + Todo(id, title) }
        return id
    }

    override suspend fun toggle(id: TodoId) {
        state.update { list -> list.map { if (it.id == id) it.copy(done = !it.done) else it } }
    }
}
