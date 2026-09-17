package tech.stit.mobiletemplate.feature.todos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.stit.mobiletemplate.core.designsystem.StitTheme
import tech.stit.mobiletemplate.core.model.Todo
import tech.stit.mobiletemplate.core.model.TodoId

@Composable
fun TodosScreen(modifier: Modifier = Modifier, viewModel: TodosViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodosScreen(state = state, onToggle = viewModel::onToggle, modifier = modifier)
}

@Composable
internal fun TodosScreen(state: TodosUiState, onToggle: (TodoId) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(StitTheme.spacing.md)) {
        Text(text = "Задачи", style = MaterialTheme.typography.headlineSmall)
        when (state) {
            TodosUiState.Loading -> CircularProgressIndicator(Modifier.semantics { testTag = "loading" })

            is TodosUiState.Failed -> Text(text = state.message, color = MaterialTheme.colorScheme.error)

            is TodosUiState.Ready -> LazyColumn {
                items(state.items, key = { it.id.value }) { todo ->
                    TodoRow(todo = todo, onToggle = onToggle, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun TodoRow(todo: Todo, onToggle: (TodoId) -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = todo.title,
        modifier = modifier
            .clickable { onToggle(todo.id) }
            .padding(vertical = StitTheme.spacing.sm),
        color = if (todo.done) StitTheme.semantic.doneText else MaterialTheme.colorScheme.onSurface,
        textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
    )
}

@Preview(showBackground = true)
@Composable
internal fun TodosScreenReadyPreview() {
    StitTheme {
        TodosScreen(
            state = TodosUiState.Ready(
                listOf(
                    Todo(TodoId("1"), "Прочитать бриф"),
                    Todo(TodoId("2"), "Собрать гейт", done = true),
                ),
            ),
            onToggle = {},
        )
    }
}
