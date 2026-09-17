package tech.stit.mobiletemplate.feature.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.stit.mobiletemplate.core.data.TodoRepository
import tech.stit.mobiletemplate.core.model.Todo
import tech.stit.mobiletemplate.core.model.TodoId
import tech.stit.mobiletemplate.core.model.sortedForDisplay
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class TodosViewModel
@Inject
constructor(
    private val repository: TodoRepository,
) : ViewModel() {
    val uiState: StateFlow<TodosUiState> =
        repository.todos
            .map<List<Todo>, TodosUiState> { TodosUiState.Ready(it.sortedForDisplay()) }
            .catch { emit(TodosUiState.Failed(it.message ?: "unknown")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TodosUiState.Loading)

    fun onToggle(id: TodoId) {
        viewModelScope.launch { repository.toggle(id) }
    }

    fun onAdd(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.add(title) }
    }
}
