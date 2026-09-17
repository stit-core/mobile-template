package tech.stit.mobile.android.feature.todos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitButton
import tech.stit.mobile.android.core.designsystem.component.StitButtonVariant
import tech.stit.mobile.android.core.designsystem.component.StitStatusBadge
import tech.stit.mobile.android.core.designsystem.component.StitTone
import tech.stit.mobile.android.core.designsystem.component.StitTopBar
import tech.stit.mobile.android.core.ui.ErrorState
import tech.stit.mobile.android.core.ui.LoadingState
import tech.stit.mobile.android.core.ui.OfflineBanner
import tech.stit.mobile.android.core.ui.messageText
import tech.stit.mobile.android.core.ui.toDateTimeText
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.entity.todo.Todo
import tech.stit.mobile.feature.todos.TodoDetailIntent
import tech.stit.mobile.feature.todos.TodoDetailState

@Composable
fun TodoDetailScreen(state: TodoDetailState, onIntent: (TodoDetailIntent) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier, topBar = {
        StitTopBar(title = stringResource(R.string.feature_todos_detail_title), onBack = onBack)
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.offline) OfflineBanner()
            val todo = state.todo
            when {
                todo != null -> TodoDetails(todo = todo, state = state, onIntent = onIntent)

                state.loading -> LoadingState()

                else -> ErrorState(message = messageText(state.error ?: MessageKey.UnknownError), onRetry = {
                    onIntent(TodoDetailIntent.Start)
                })
            }
        }
    }
}

@Composable
private fun TodoDetails(todo: Todo, state: TodoDetailState, onIntent: (TodoDetailIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(StitTheme.spacing.md)) {
        Text(text = todo.title, style = StitTheme.typography.headingLg)
        StitStatusBadge(
            text = stringResource(if (todo.done) R.string.feature_todos_status_done else R.string.feature_todos_status_open),
            tone = if (todo.done) StitTone.Success else StitTone.Warning,
            modifier = Modifier.padding(top = StitTheme.spacing.sm),
        )
        Text(
            text = stringResource(R.string.feature_todos_created, todo.createdAt.toDateTimeText()),
            modifier = Modifier.padding(top = StitTheme.spacing.md),
            style = StitTheme.typography.bodySm,
            color = StitTheme.colors.muted,
        )
        Text(
            text = stringResource(R.string.feature_todos_updated, todo.updatedAt.toDateTimeText()),
            style = StitTheme.typography.bodySm,
            color = StitTheme.colors.muted,
        )
        state.error?.let { error ->
            Text(text = messageText(error), modifier = Modifier.padding(top = StitTheme.spacing.sm), color = StitTheme.colors.danger)
        }
        StitButton(
            text = stringResource(if (todo.done) R.string.feature_todos_mark_open else R.string.feature_todos_mark_done),
            onClick = { onIntent(TodoDetailIntent.Toggle) },
            modifier = Modifier.fillMaxWidth().padding(top = StitTheme.spacing.lg).testTag(TodosTestTags.TOGGLE),
            enabled = !state.offline,
        )
        StitButton(
            text = stringResource(R.string.feature_todos_delete_task),
            onClick = { onIntent(TodoDetailIntent.Delete) },
            modifier = Modifier.fillMaxWidth().padding(top = StitTheme.spacing.sm).testTag(TodosTestTags.DELETE),
            variant = StitButtonVariant.Secondary,
            enabled = !state.offline,
        )
    }
}
