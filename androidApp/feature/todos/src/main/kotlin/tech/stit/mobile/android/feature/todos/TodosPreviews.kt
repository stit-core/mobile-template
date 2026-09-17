package tech.stit.mobile.android.feature.todos

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.entity.todo.Todo
import tech.stit.mobile.feature.todos.TodoDetailState
import tech.stit.mobile.feature.todos.TodoListState
import kotlin.time.Instant

private val previewTodos = listOf(
    Todo(
        "todo-1",
        "Прочитать docs/reference/architecture.md",
        false,
        Instant.parse("2026-09-10T08:00:00Z"),
        Instant.parse("2026-09-10T08:00:00Z"),
        1,
    ),
    Todo("todo-2", "Собрать demo-сборку", true, Instant.parse("2026-09-11T09:30:00Z"), Instant.parse("2026-09-12T10:15:00Z"), 2),
)

@Preview(showBackground = true)
@Composable
private fun TodoListPopulatedPreview() {
    StitTheme { TodoListScreen(state = TodoListState(todos = previewTodos), onIntent = {}, onOpenTodo = {}) }
}

@Preview(showBackground = true)
@Composable
private fun TodoListEmptyPreview() {
    StitTheme { TodoListScreen(state = TodoListState(), onIntent = {}, onOpenTodo = {}) }
}

@Preview(showBackground = true)
@Composable
private fun TodoListErrorOfflinePreview() {
    StitTheme { TodoListScreen(state = TodoListState(error = MessageKey.NetworkError, offline = true), onIntent = {}, onOpenTodo = {}) }
}

@Preview(showBackground = true)
@Composable
private fun TodoDetailPreview() {
    StitTheme { TodoDetailScreen(state = TodoDetailState(todo = previewTodos.first()), onIntent = {}, onBack = {}) }
}
