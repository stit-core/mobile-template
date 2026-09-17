package tech.stit.mobile.android.feature.todos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.ui.UiTestTags
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.feature.todos.TodoListIntent
import tech.stit.mobile.feature.todos.TodoListState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class TodoListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<TodoListIntent>()
    private val opened = mutableListOf<String>()

    private fun show(state: TodoListState) {
        composeRule.setContent { StitTheme { TodoListScreen(state = state, onIntent = intents::add, onOpenTodo = opened::add) } }
    }

    @Test
    fun `загрузка без данных`() {
        show(TodoListState(loading = true))
        composeRule.onNodeWithTag(UiTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun `пустой список — подсказка`() {
        show(TodoListState())
        composeRule.onNodeWithTag(UiTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No tasks yet").assertIsDisplayed()
    }

    @Test
    fun `ошибка без данных — повтор зовёт Refresh`() {
        show(TodoListState(error = MessageKey("error.internal")))
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<TodoListIntent>(TodoListIntent.Refresh), intents)
    }

    @Test
    fun `список — переключение, открытие, удаление, создание`() {
        show(TodoListState(todos = demoTodos, newTitle = "Новая"))
        composeRule.onAllNodesWithTag(TodosTestTags.TOGGLE)[0].performClick()
        composeRule.onNodeWithTag(TodosTestTags.item("todo-3")).performClick()
        composeRule.onAllNodesWithTag(TodosTestTags.DELETE)[1].performClick()
        composeRule.onNodeWithTag(TodosTestTags.NEW_TITLE).performTextInput("!")
        composeRule.onNodeWithTag(TodosTestTags.ADD).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("todo-3"), opened)
        assertTrue(intents.contains(TodoListIntent.Toggle("todo-1")), intents.toString())
        assertTrue(intents.contains(TodoListIntent.Delete("todo-2")), intents.toString())
        assertTrue(intents.any { it is TodoListIntent.TitleChanged }, intents.toString())
        assertEquals(TodoListIntent.Create, intents.last())
    }

    @Test
    fun `офлайн — только чтение`() {
        show(TodoListState(todos = demoTodos, newTitle = "Новая", offline = true))
        composeRule.onNodeWithTag(UiTestTags.OFFLINE_BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(TodosTestTags.NEW_TITLE).assertIsNotEnabled()
        composeRule.onNodeWithTag(TodosTestTags.ADD).assertIsNotEnabled()
        composeRule.onAllNodes(hasTestTag(TodosTestTags.DELETE))[0].assertIsNotEnabled()
    }
}
