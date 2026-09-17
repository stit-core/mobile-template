package tech.stit.mobiletemplate.feature.todos

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.stit.mobiletemplate.core.designsystem.StitTheme
import tech.stit.mobiletemplate.core.model.Todo
import tech.stit.mobiletemplate.core.model.TodoId

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w360dp-h640dp-xhdpi")
class TodosScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule =
        RoborazziRule(
            options = RoborazziRule.Options(outputDirectoryPath = "src/test/screenshots"),
        )

    @Test
    fun todosScreenReady() {
        composeRule.setContent {
            StitTheme(darkTheme = false) {
                TodosScreen(
                    state =
                    TodosUiState.Ready(
                        listOf(
                            Todo(TodoId("1"), "Прочитать бриф"),
                            Todo(TodoId("2"), "Собрать гейт", done = true),
                        ),
                    ),
                    onToggle = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/todos_ready.png")
    }

    @Test
    fun todosScreenLoading() {
        composeRule.setContent {
            StitTheme(darkTheme = true) {
                TodosScreen(state = TodosUiState.Loading, onToggle = {})
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/todos_loading_dark.png")
    }
}
