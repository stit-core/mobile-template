package tech.stit.mobile.android.core.designsystem.component

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.android.core.designsystem.StitTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `клик вызывает onClick`() {
        var clicks = 0
        composeRule.setContent { StitTheme { StitButton(text = "Go", onClick = { clicks++ }) } }
        composeRule.onNodeWithText("Go").performClick()
        composeRule.waitForIdle()
        assertEquals(1, clicks)
    }

    @Test
    fun `во время загрузки кнопка выключена и текст скрыт`() {
        composeRule.setContent { StitTheme { StitButton(text = "Go", onClick = {}, loading = true) } }
        composeRule.onNodeWithText("Go").assertDoesNotExist()
        composeRule.onNode(hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun `поле ввода отдаёт введённый текст и показывает ошибку`() {
        var value = ""
        composeRule.setContent {
            StitTheme { StitTextField(value = value, onValueChange = { value = it }, label = "Email", error = "Обязательное поле") }
        }
        composeRule.onNodeWithText("Email").performTextInput("a@b.c")
        composeRule.waitForIdle()
        assertTrue(value.startsWith("a@b.c"), value)
        composeRule.onNodeWithText("Обязательное поле").assertExists()
    }
}
