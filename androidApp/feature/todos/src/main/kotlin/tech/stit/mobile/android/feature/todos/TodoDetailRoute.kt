package tech.stit.mobile.android.feature.todos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TodoDetailRoute(
    id: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodoDetailViewModel = hiltViewModel<TodoDetailViewModel, TodoDetailViewModel.Factory>(creationCallback = { it.create(id) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    // Удалённой задаче экран не нужен: возврат к списку — навигация из состояния, не событие.
    LaunchedEffect(state.deleted) { if (state.deleted) currentOnBack() }
    TodoDetailScreen(state = state, onIntent = viewModel::dispatch, onBack = onBack, modifier = modifier)
}
