package tech.stit.mobile.android.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.feature.auth.DeleteAccountIntent
import tech.stit.mobile.feature.auth.DeleteAccountState
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class DeleteAccountScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<DeleteAccountIntent>()

    @Test
    fun `без подтверждения удалить нельзя, галочка уходит интентом`() {
        composeRule.setContent { StitTheme { DeleteAccountScreen(state = DeleteAccountState(), onIntent = intents::add, onBack = {}) } }
        composeRule.onNodeWithTag(AuthTestTags.DELETE_SUBMIT).assertIsNotEnabled()
        composeRule.onNodeWithTag(AuthTestTags.DELETE_CONFIRM).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<DeleteAccountIntent>(DeleteAccountIntent.ConfirmChanged(true)), intents)
    }

    @Test
    fun `подтверждённое удаление отправляется, ошибка показывается`() {
        val state = DeleteAccountState(confirmed = true, error = MessageKey("error.internal"))
        composeRule.setContent { StitTheme { DeleteAccountScreen(state = state, onIntent = intents::add, onBack = {}) } }
        composeRule.onNodeWithText("Server error. Try again later.").assertIsDisplayed()
        composeRule.onNodeWithTag(AuthTestTags.DELETE_SUBMIT).assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<DeleteAccountIntent>(DeleteAccountIntent.Delete), intents)
    }

    @Test
    fun `после удаления — подтверждающий текст`() {
        composeRule.setContent {
            StitTheme {
                DeleteAccountScreen(state = DeleteAccountState(confirmed = true, deleted = true), onIntent = intents::add, onBack = {})
            }
        }
        composeRule.onNodeWithText("Account deleted").assertIsDisplayed()
    }
}
