package tech.stit.mobile.android.feature.todos

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.feature.todos.TodoListIntent
import tech.stit.mobile.feature.todos.TodoListState
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(container: SharedContainer) : ViewModel() {
    private val presenter = container.todoListPresenter()
    val state: StateFlow<TodoListState> = presenter.state

    init {
        presenter.dispatch(TodoListIntent.Start)
    }

    fun dispatch(intent: TodoListIntent) = presenter.dispatch(intent)

    override fun onCleared() = presenter.close()
}
