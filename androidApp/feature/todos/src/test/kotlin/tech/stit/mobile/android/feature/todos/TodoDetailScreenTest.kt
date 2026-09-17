package tech.stit.mobile.android.feature.todos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.ui.UiTestTags
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.feature.todos.TodoDetailIntent
import tech.stit.mobile.feature.todos.TodoDetailState
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class TodoDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<TodoDetailIntent>()

    private fun show(state: TodoDetailState, onBack: () -> Unit = {}) {
        composeRule.setContent { StitTheme { TodoDetailScreen(state = state, onIntent = intents::add, onBack = onBack) } }
    }

    @Test
    fun `загрузка`() {
        show(TodoDetailState(loading = true))
        composeRule.onNodeWithTag(UiTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun `ошибка — повтор зовёт Start`() {
        show(TodoDetailState(error = MessageKey("error.internal")))
        composeRule.onNodeWithText("Server error. Try again later.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<TodoDetailIntent>(TodoDetailIntent.Start), intents)
    }

    @Test
    fun `задача — переключение, удаление, назад`() {
        var backs = 0
        show(TodoDetailState(todo = demoTodos[1]), onBack = { backs++ })
        composeRule.onNodeWithText("Done").assertIsDisplayed()
        composeRule.onNodeWithTag(TodosTestTags.TOGGLE).performClick()
        composeRule.onNodeWithTag(TodosTestTags.DELETE).performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(TodoDetailIntent.Toggle, TodoDetailIntent.Delete), intents)
        assertEquals(1, backs)
    }

    @Test
    fun `офлайн — действия недоступны`() {
        show(TodoDetailState(todo = demoTodos[0], offline = true))
        composeRule.onNodeWithTag(UiTestTags.OFFLINE_BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(TodosTestTags.TOGGLE).assertIsNotEnabled()
        composeRule.onNodeWithTag(TodosTestTags.DELETE).assertIsNotEnabled()
    }
}
