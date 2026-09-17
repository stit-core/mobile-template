package tech.stit.mobiletemplate.feature.todos

import tech.stit.mobiletemplate.core.model.Todo

sealed interface TodosUiState {
    data object Loading : TodosUiState

    data class Ready(
        val items: List<Todo>,
    ) : TodosUiState

    data class Failed(
        val message: String,
    ) : TodosUiState
}
