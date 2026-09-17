package tech.stit.mobile.android.core.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.core.i18n.KnownErrorCodes
import tech.stit.mobile.core.i18n.MessageKey
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(AndroidJUnit4::class)
class StatesTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `ошибка показывает текст и повтор зовёт onRetry`() {
        var retries = 0
        composeRule.setContent { StitTheme { ErrorState(message = "boom", onRetry = { retries++ }) } }
        composeRule.onNodeWithTag(UiTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()
        assertEquals(1, retries)
    }

    @Test
    fun `пустое состояние зовёт onAction`() {
        var actions = 0
        composeRule.setContent { StitTheme { EmptyState(title = "Empty", actionLabel = "Add", onAction = { actions++ }) } }
        composeRule.onNodeWithText("Add").performClick()
        composeRule.waitForIdle()
        assertEquals(1, actions)
    }

    @Test
    fun `нужно обновление — кнопка открывает магазин`() {
        var opened: String? = null
        composeRule.setContent { StitTheme { UpdateRequired(storeUrl = "https://store.example", onOpenStore = { opened = it }) } }
        composeRule.onNodeWithText("Update").performClick()
        composeRule.waitForIdle()
        assertEquals("https://store.example", opened)
    }

    @Test
    fun `технические работы — повтор зовёт onRetry`() {
        var retries = 0
        composeRule.setContent { StitTheme { Maintenance(until = null, onRetry = { retries++ }) } }
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()
        assertEquals(1, retries)
    }

    @Test
    fun `каждый известный код ошибки имеет свою строку, неизвестный — общую`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val known = KnownErrorCodes.all.map { MessageKey.forErrorCode(it) } +
            listOf(MessageKey.NetworkError, MessageKey.ValidationError)
        known.forEach { key ->
            assertNotEquals(R.string.core_ui_error_unknown, key.stringRes(), key.value)
            assertNotEquals("", context.getString(key.stringRes()))
        }
        assertEquals(R.string.core_ui_error_unknown, MessageKey("error.something_new").stringRes())
    }
}
