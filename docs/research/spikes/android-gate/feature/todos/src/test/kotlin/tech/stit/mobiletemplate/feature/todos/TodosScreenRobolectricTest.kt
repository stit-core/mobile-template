package tech.stit.mobiletemplate.feature.todos

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import tech.stit.mobiletemplate.core.designsystem.StitTheme
import tech.stit.mobiletemplate.core.model.Todo
import tech.stit.mobiletemplate.core.model.TodoId

@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class TodosScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `элементы списка отрисованы`() {
        composeRule.setContent {
            StitTheme {
                TodosScreen(
                    state =
                    TodosUiState.Ready(
                        listOf(Todo(TodoId("1"), "alpha"), Todo(TodoId("2"), "beta", done = true)),
                    ),
                    onToggle = {},
                )
            }
        }
        composeRule.onNodeWithText("alpha").assertExists()
        composeRule.onNodeWithText("beta").assertExists()
    }

    @Test
    fun `клик по строке зовёт onToggle`() {
        var toggled: TodoId? = null
        composeRule.setContent {
            StitTheme {
                TodosScreen(
                    state = TodosUiState.Ready(listOf(Todo(TodoId("42"), "alpha"))),
                    onToggle = { toggled = it },
                )
            }
        }
        composeRule.onNodeWithText("alpha").performClickCompat()
        composeRule.waitForIdle()
        assert(toggled == TodoId("42")) { "onToggle не вызван, got=$toggled" }
    }
}
