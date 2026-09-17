package tech.stit.mobile.android.feature.todos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import tech.stit.mobile.android.core.designsystem.StitIcons
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitTextField
import tech.stit.mobile.android.core.designsystem.component.StitTopBar
import tech.stit.mobile.android.core.ui.EmptyState
import tech.stit.mobile.android.core.ui.ErrorState
import tech.stit.mobile.android.core.ui.LoadingState
import tech.stit.mobile.android.core.ui.OfflineBanner
import tech.stit.mobile.android.core.ui.messageText
import tech.stit.mobile.entity.todo.Todo
import tech.stit.mobile.feature.todos.TodoListIntent
import tech.stit.mobile.feature.todos.TodoListState

@Composable
fun TodoListScreen(state: TodoListState, onIntent: (TodoListIntent) -> Unit, onOpenTodo: (String) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            StitTopBar(title = stringResource(R.string.feature_todos_title)) {
                IconButton(onClick = { onIntent(TodoListIntent.Refresh) }) {
                    Icon(painter = painterResource(StitIcons.Refresh), contentDescription = stringResource(R.string.feature_todos_refresh))
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.offline) OfflineBanner()
            NewTodoRow(state = state, onIntent = onIntent, modifier = Modifier.padding(horizontal = StitTheme.spacing.md))
            val error = state.error
            when {
                state.loading && state.todos.isEmpty() -> LoadingState()

                error != null && state.todos.isEmpty() -> ErrorState(message = messageText(error), onRetry = {
                    onIntent(TodoListIntent.Refresh)
                })

                state.todos.isEmpty() -> EmptyState(
                    title = stringResource(R.string.feature_todos_empty_title),
                    description = stringResource(R.string.feature_todos_empty_description),
                )

                else -> TodoList(state = state, onIntent = onIntent, onOpenTodo = onOpenTodo)
            }
        }
    }
}

@Composable
private fun NewTodoRow(state: TodoListState, onIntent: (TodoListIntent) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        StitTextField(
            value = state.newTitle,
            onValueChange = { onIntent(TodoListIntent.TitleChanged(it)) },
            label = stringResource(R.string.feature_todos_new_title),
            modifier = Modifier.weight(1f).testTag(TodosTestTags.NEW_TITLE),
            // Офлайн — только чтение (TodoListState.offline): мутации без сети не ставятся в очередь.
            enabled = !state.offline,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (state.canCreate) onIntent(TodoListIntent.Create) }),
        )
        IconButton(onClick = {
            onIntent(TodoListIntent.Create)
        }, modifier = Modifier.testTag(TodosTestTags.ADD), enabled = state.canCreate) {
            Icon(painter = painterResource(StitIcons.Add), contentDescription = stringResource(R.string.feature_todos_add))
        }
    }
}

@Composable
private fun TodoList(
    state: TodoListState,
    onIntent: (TodoListIntent) -> Unit,
    onOpenTodo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.testTag(TodosTestTags.LIST)) {
        state.error?.let { error ->
            item { Text(text = messageText(error), modifier = Modifier.padding(StitTheme.spacing.md), color = StitTheme.colors.danger) }
        }
        items(state.todos, key = { it.id }) { todo ->
            TodoRow(
                todo = todo,
                enabled = !state.offline,
                onToggle = { onIntent(TodoListIntent.Toggle(todo.id)) },
                onDelete = { onIntent(TodoListIntent.Delete(todo.id)) },
                onOpen = { onOpenTodo(todo.id) },
            )
        }
    }
}

@Composable
private fun TodoRow(
    todo: Todo,
    enabled: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = StitTheme.spacing.touchTarget)
            .clickable(onClick = onOpen)
            .padding(start = StitTheme.spacing.sm, end = StitTheme.spacing.xs)
            .testTag(TodosTestTags.item(todo.id)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = todo.done, onCheckedChange = {
            onToggle()
        }, modifier = Modifier.testTag(TodosTestTags.TOGGLE), enabled = enabled)
        Text(
            text = todo.title,
            modifier = Modifier.weight(1f),
            color = if (todo.done) StitTheme.colors.muted else StitTheme.colors.appFg,
            textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
        )
        IconButton(onClick = onDelete, modifier = Modifier.testTag(TodosTestTags.DELETE), enabled = enabled) {
            Icon(painter = painterResource(StitIcons.Delete), contentDescription = stringResource(R.string.feature_todos_delete))
        }
    }
}
