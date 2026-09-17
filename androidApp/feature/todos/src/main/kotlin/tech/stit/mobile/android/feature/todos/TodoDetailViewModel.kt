package tech.stit.mobile.android.feature.todos

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.feature.todos.TodoDetailIntent
import tech.stit.mobile.feature.todos.TodoDetailState

// id приходит из NavKey записи back stack (Destination.TodoDetail), а не из SavedStateHandle — assisted-фабрика.
@HiltViewModel(assistedFactory = TodoDetailViewModel.Factory::class)
class TodoDetailViewModel @AssistedInject constructor(container: SharedContainer, @Assisted id: String) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(id: String): TodoDetailViewModel
    }

    private val presenter = container.todoDetailPresenter(id)
    val state: StateFlow<TodoDetailState> = presenter.state

    init {
        presenter.dispatch(TodoDetailIntent.Start)
    }

    fun dispatch(intent: TodoDetailIntent) = presenter.dispatch(intent)

    override fun onCleared() = presenter.close()
}
