package tech.stit.mobiletemplate.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tech.stit.mobiletemplate.core.model.Todo
import tech.stit.mobiletemplate.core.model.TodoId
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryTodoRepository @Inject constructor() : TodoRepository {
    private val counter = AtomicLong(0)
    private val state =
        MutableStateFlow(
            listOf(
                Todo(TodoId(nextId()), "Прочитать бриф"),
                Todo(TodoId(nextId()), "Собрать гейт", done = true),
            ),
        )

    override val todos: Flow<List<Todo>> = state.asStateFlow()

    override suspend fun add(title: String): TodoId {
        val id = TodoId(nextId())
        state.update { it + Todo(id, title) }
        return id
    }

    override suspend fun toggle(id: TodoId) {
        state.update { list -> list.map { if (it.id == id) it.copy(done = !it.done) else it } }
    }

    private fun nextId(): String = "todo-${counter.incrementAndGet()}"
}
