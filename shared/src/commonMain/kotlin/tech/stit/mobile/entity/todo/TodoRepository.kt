package tech.stit.mobile.entity.todo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.stit.mobile.core.api.ApiError
import tech.stit.mobile.core.api.TodosApi
import tech.stit.mobile.core.api.generated.TodoView
import tech.stit.mobile.core.lib.FileCache
import kotlin.coroutines.cancellation.CancellationException

// SSOT задач: список в памяти + снимок последнего ответа в FileCache (офлайн — только чтение).
class TodoRepository internal constructor(
    private val api: TodosApi,
    private val cache: FileCache<List<TodoView>>,
) {
    private var views: List<TodoView> = emptyList()
    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()

    suspend fun restore() {
        cache.read()?.let { publish(it) }
    }

    @Throws(ApiError::class, CancellationException::class)
    suspend fun refresh(): List<Todo> {
        publish(api.list().data)
        return _todos.value
    }

    @Throws(ApiError::class, CancellationException::class)
    suspend fun get(id: String): Todo = (views.firstOrNull { it.id == id } ?: api.get(id).data.also { upsert(it) }).toModel()

    @Throws(ApiError::class, CancellationException::class)
    suspend fun create(title: String): Todo = api.create(title).data.also { upsert(it) }.toModel()

    @Throws(ApiError::class, CancellationException::class)
    suspend fun toggle(id: String): Todo {
        val current = views.firstOrNull { it.id == id } ?: api.get(id).data
        return api.update(id, current.version, done = !current.done).data.also { upsert(it) }.toModel()
    }

    @Throws(ApiError::class, CancellationException::class)
    suspend fun delete(id: String) {
        api.delete(id)
        publish(views.filterNot { it.id == id })
    }

    suspend fun clear() {
        views = emptyList()
        _todos.value = emptyList()
        cache.clear()
    }

    private suspend fun upsert(view: TodoView) =
        publish(if (views.any { it.id == view.id }) views.map { if (it.id == view.id) view else it } else listOf(view) + views)

    private suspend fun publish(fresh: List<TodoView>) {
        views = fresh
        _todos.value = fresh.map { it.toModel() }
        cache.write(fresh)
    }
}
