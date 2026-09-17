package tech.stit.mobile.feature.todos

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.stit.mobile.core.api.toMessageKey
import tech.stit.mobile.core.lib.ConnectivityMonitor
import tech.stit.mobile.core.lib.DispatcherProvider
import tech.stit.mobile.core.lib.attempt
import tech.stit.mobile.core.lib.presenterScope
import tech.stit.mobile.entity.todo.TodoRepository

// Экран задачи по id — цель deep link https://<host>/todos/{id}.
class TodoDetailPresenter internal constructor(
    private val id: String,
    private val todos: TodoRepository,
    private val connectivity: ConnectivityMonitor,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(TodoDetailState())
    val state: StateFlow<TodoDetailState> = _state.asStateFlow()

    fun dispatch(intent: TodoDetailIntent) {
        when (intent) {
            TodoDetailIntent.Start -> start()

            TodoDetailIntent.Toggle -> mutate { todos.toggle(id) }

            TodoDetailIntent.Delete -> mutate {
                todos.delete(id)
                _state.update { it.copy(deleted = true) }
            }
        }
    }

    fun close() = scope.cancel()

    private fun start() {
        scope.launch { connectivity.online.collect { online -> _state.update { it.copy(offline = !online) } } }
        scope.launch {
            todos.todos.collect { list -> list.firstOrNull { it.id == id }?.let { todo -> _state.update { it.copy(todo = todo) } } }
        }
        _state.update { it.copy(loading = true, error = null) }
        scope.launch {
            attempt { todos.get(id) }
                .onSuccess { todo -> _state.update { it.copy(loading = false, todo = todo) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toMessageKey()) } }
        }
    }

    private fun mutate(action: suspend () -> Unit) {
        if (_state.value.offline) return
        scope.launch {
            attempt { action() }.onFailure { e -> _state.update { it.copy(error = e.toMessageKey()) } }
        }
    }
}
