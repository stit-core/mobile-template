package tech.stit.mobile.android.feature.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.ui.UiTestTags
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.feature.profile.ProfileIntent
import tech.stit.mobile.feature.profile.ProfileState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ProfileIntent>()

    private fun show(state: ProfileState, onOpenSessions: () -> Unit = {}, onOpenDeleteAccount: () -> Unit = {}) {
        composeRule.setContent {
            StitTheme {
                ProfileScreen(
                    state = state,
                    onIntent = intents::add,
                    onOpenSessions = onOpenSessions,
                    onOpenDeleteAccount = onOpenDeleteAccount,
                )
            }
        }
    }

    @Test
    fun `без пользователя — загрузка, ошибка даёт повтор`() {
        show(ProfileState(loading = true))
        composeRule.onNodeWithTag(UiTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun `ошибка без пользователя — повтор зовёт Refresh`() {
        show(ProfileState(error = MessageKey.NetworkError))
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<ProfileIntent>(ProfileIntent.Refresh), intents)
    }

    @Test
    fun `имя редактируется и сохраняется, смена пароля уходит парой`() {
        show(ProfileState(user = demoUser, draftName = "Новое имя"))
        composeRule.onNodeWithTag(ProfileTestTags.NAME).performTextInput("!")
        composeRule.onNodeWithTag(ProfileTestTags.SAVE).assertIsEnabled().performClick()
        composeRule.onNodeWithTag(ProfileTestTags.CHANGE_PASSWORD).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag(ProfileTestTags.OLD_PASSWORD).performScrollTo().performTextInput("old")
        composeRule.onNodeWithTag(ProfileTestTags.NEW_PASSWORD).performScrollTo().performTextInput("new")
        composeRule.onNodeWithTag(ProfileTestTags.CHANGE_PASSWORD).performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertTrue(intents.any { it is ProfileIntent.NameChanged }, intents.toString())
        assertTrue(intents.contains(ProfileIntent.SaveName), intents.toString())
        assertTrue(intents.contains(ProfileIntent.ChangePassword("old", "new")), intents.toString())
    }

    @Test
    fun `ссылки, выход и статус смены пароля`() {
        var sessions = 0
        var deletes = 0
        show(ProfileState(user = demoUser, draftName = demoUser.name, passwordChanged = true), { sessions++ }, { deletes++ })
        composeRule.onNodeWithText("Password changed").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(ProfileTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithText("Sessions").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete account").performScrollTo().performClick()
        composeRule.onNodeWithText("Sign out").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, sessions)
        assertEquals(1, deletes)
        assertEquals(listOf<ProfileIntent>(ProfileIntent.Logout), intents)
    }
}
