package tech.stit.mobile.android.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
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
import tech.stit.mobile.core.push.PushStatus
import tech.stit.mobile.feature.push.PushOptInIntent
import tech.stit.mobile.feature.push.PushOptInState
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class PushOptInScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<PushOptInIntent>()

    private fun show(state: PushOptInState) {
        composeRule.setContent { StitTheme { PushOptInScreen(state = state, onIntent = intents::add, onBack = {}) } }
    }

    @Test
    fun `не спрашивали — включить и не сейчас`() {
        show(PushOptInState(status = PushStatus.NotAsked))
        composeRule.onNodeWithText("Not enabled yet").assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.PUSH_ACCEPT).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.PUSH_DECLINE).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(PushOptInIntent.Accept, PushOptInIntent.Decline), intents)
    }

    @Test
    fun `включены — только отключить`() {
        show(PushOptInState(status = PushStatus.Granted))
        composeRule.onNodeWithTag(SettingsTestTags.PUSH_ACCEPT).assertDoesNotExist()
        composeRule.onNodeWithTag(SettingsTestTags.PUSH_DECLINE).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<PushOptInIntent>(PushOptInIntent.Decline), intents)
    }

    @Test
    fun `занято — кнопки выключены, ошибка видна`() {
        show(PushOptInState(status = PushStatus.Declined, busy = true, error = MessageKey.NetworkError))
        composeRule.onNodeWithTag(SettingsTestTags.PUSH_ACCEPT).assertIsNotEnabled()
        composeRule.onNodeWithTag(SettingsTestTags.PUSH_DECLINE).assertIsNotEnabled()
        composeRule.onNodeWithText("No connection. Check your network and try again.").assertIsDisplayed()
    }
}
