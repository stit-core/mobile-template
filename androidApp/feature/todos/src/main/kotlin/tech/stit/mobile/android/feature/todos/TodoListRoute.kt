package tech.stit.mobile.android.feature.todos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TodoListRoute(onOpenTodo: (String) -> Unit, modifier: Modifier = Modifier, viewModel: TodoListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TodoListScreen(state = state, onIntent = viewModel::dispatch, onOpenTodo = onOpenTodo, modifier = modifier)
}
