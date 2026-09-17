package tech.stit.mobile.android.feature.todos

import org.junit.Test
import tech.stit.mobile.android.core.testing.ScreenshotTest
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.feature.todos.TodoDetailState
import tech.stit.mobile.feature.todos.TodoListState

class TodosScreenshotTest : ScreenshotTest() {
    @Test
    fun `список — загрузка`() = capture("todo_list_loading") { List(TodoListState(loading = true)) }

    @Test
    fun `список — пусто`() = capture("todo_list_empty") { List(TodoListState()) }

    @Test
    fun `список — задачи`() = capture("todo_list_populated") { List(TodoListState(todos = demoTodos, newTitle = "Новая задача")) }

    @Test
    fun `список — задачи в тёмной теме`() = capture("todo_list_populated_dark", darkTheme = true) { List(TodoListState(todos = demoTodos)) }

    @Test
    fun `список — ошибка`() = capture("todo_list_error") { List(TodoListState(error = MessageKey("error.internal"))) }

    @Test
    fun `список — офлайн`() = capture("todo_list_offline") { List(TodoListState(todos = demoTodos, offline = true)) }

    @Test
    fun `задача — загрузка`() = capture("todo_detail_loading") { Detail(TodoDetailState(loading = true)) }

    @Test
    fun `задача — в работе`() = capture("todo_detail_populated") { Detail(TodoDetailState(todo = demoTodos[0])) }

    @Test
    fun `задача — сделана`() = capture("todo_detail_done") { Detail(TodoDetailState(todo = demoTodos[1])) }

    @Test
    fun `задача — ошибка`() = capture("todo_detail_error") { Detail(TodoDetailState(error = MessageKey("error.internal"))) }

    @Test
    fun `задача — офлайн`() = capture("todo_detail_offline") { Detail(TodoDetailState(todo = demoTodos[0], offline = true)) }
}

@androidx.compose.runtime.Composable
private fun List(state: TodoListState) = TodoListScreen(state = state, onIntent = {}, onOpenTodo = {})

@androidx.compose.runtime.Composable
private fun Detail(state: TodoDetailState) = TodoDetailScreen(state = state, onIntent = {}, onBack = {})
