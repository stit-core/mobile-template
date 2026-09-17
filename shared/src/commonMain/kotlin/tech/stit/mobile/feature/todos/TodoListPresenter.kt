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

class TodoListPresenter internal constructor(
    private val todos: TodoRepository,
    private val connectivity: ConnectivityMonitor,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(TodoListState())
    val state: StateFlow<TodoListState> = _state.asStateFlow()

    fun dispatch(intent: TodoListIntent) {
        when (intent) {
            TodoListIntent.Start -> start()
            TodoListIntent.Refresh -> refresh()
            is TodoListIntent.TitleChanged -> _state.update { it.copy(newTitle = intent.value, error = null) }
            TodoListIntent.Create -> create()
            is TodoListIntent.Toggle -> mutate { todos.toggle(intent.id) }
            is TodoListIntent.Delete -> mutate { todos.delete(intent.id) }
        }
    }

    fun close() = scope.cancel()

    private fun start() {
        scope.launch { todos.todos.collect { list -> _state.update { it.copy(todos = list) } } }
        scope.launch { connectivity.online.collect { online -> _state.update { it.copy(offline = !online) } } }
        refresh()
    }

    private fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        scope.launch {
            attempt { todos.refresh() }
                .onSuccess { _state.update { it.copy(loading = false) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toMessageKey()) } }
        }
    }

    private fun create() {
        val title = _state.value.newTitle.trim()
        if (!_state.value.canCreate) return
        _state.update { it.copy(newTitle = "") }
        mutate { todos.create(title) }
    }

    private fun mutate(action: suspend () -> Unit) {
        if (_state.value.offline) return
        scope.launch {
            attempt { action() }.onFailure { e -> _state.update { it.copy(error = e.toMessageKey()) } }
        }
    }
}
