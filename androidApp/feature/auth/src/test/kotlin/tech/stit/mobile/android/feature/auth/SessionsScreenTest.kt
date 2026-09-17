package tech.stit.mobile.android.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
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
import tech.stit.mobile.feature.auth.SessionsIntent
import tech.stit.mobile.feature.auth.SessionsState
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SessionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<SessionsIntent>()

    @Test
    fun `загрузка без данных — индикатор`() {
        composeRule.setContent { StitTheme { SessionsScreen(state = SessionsState(loading = true), onIntent = intents::add, onBack = {}) } }
        composeRule.onNodeWithTag(UiTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun `ошибка без данных — повтор зовёт Refresh`() {
        composeRule.setContent {
            StitTheme { SessionsScreen(state = SessionsState(error = MessageKey.NetworkError), onIntent = intents::add, onBack = {}) }
        }
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<SessionsIntent>(SessionsIntent.Refresh), intents)
    }

    @Test
    fun `список — завершение сессии, удаление устройства и выход везде`() {
        val state = SessionsState(sessions = listOf(currentSession, webSession), devices = listOf(fcmDevice))
        composeRule.setContent { StitTheme { SessionsScreen(state = state, onIntent = intents::add, onBack = {}) } }
        composeRule.onNodeWithText("This device").assertIsDisplayed()
        composeRule.onNodeWithText("End").performClick()
        composeRule.onNodeWithText("Remove").performClick()
        composeRule.onNodeWithText("Sign out everywhere").performClick()
        composeRule.waitForIdle()
        assertEquals(
            listOf(SessionsIntent.RevokeSession("session-web"), SessionsIntent.UnregisterDevice("dev-1"), SessionsIntent.RevokeAll),
            intents,
        )
    }

    @Test
    fun `пустой список и кнопка назад`() {
        var backs = 0
        composeRule.setContent { StitTheme { SessionsScreen(state = SessionsState(), onIntent = intents::add, onBack = { backs++ }) } }
        composeRule.onNodeWithText("No active sessions").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backs)
    }
}
