package tech.stit.mobiletemplate.core.data

import kotlinx.coroutines.flow.Flow
import tech.stit.mobiletemplate.core.model.Todo
import tech.stit.mobiletemplate.core.model.TodoId

interface TodoRepository {
    val todos: Flow<List<Todo>>

    suspend fun add(title: String): TodoId

    suspend fun toggle(id: TodoId)
}
