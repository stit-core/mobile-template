package tech.stit.mobile.android.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.feature.auth.LoginIntent
import tech.stit.mobile.feature.auth.LoginState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<LoginIntent>()

    @Test
    fun `ввод email и пароля уходит интентами, пустая форма не отправляется`() {
        composeRule.setContent { StitTheme { LoginScreen(state = LoginState(), onIntent = intents::add, onOpenSettings = {}) } }
        composeRule.onNodeWithTag(AuthTestTags.LOGIN_SUBMIT).assertIsNotEnabled()
        composeRule.onNodeWithTag(AuthTestTags.LOGIN_EMAIL).performTextInput("demo@example.com")
        composeRule.onNodeWithTag(AuthTestTags.LOGIN_PASSWORD).performTextInput("secret")
        composeRule.waitForIdle()
        assertTrue(intents.any { it is LoginIntent.EmailChanged && it.value.startsWith("demo@") }, intents.toString())
        assertTrue(intents.any { it is LoginIntent.PasswordChanged }, intents.toString())
    }

    @Test
    fun `заполненная форма отправляется, ошибка показывается текстом`() {
        val state = LoginState(email = "demo@example.com", password = "secret", error = MessageKey("error.auth_invalid_credentials"))
        composeRule.setContent { StitTheme { LoginScreen(state = state, onIntent = intents::add, onOpenSettings = {}) } }
        composeRule.onNodeWithText("Wrong email or password.").assertIsDisplayed()
        composeRule.onNodeWithTag(AuthTestTags.LOGIN_SUBMIT).assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<LoginIntent>(LoginIntent.Submit), intents)
    }

    @Test
    fun `вход в demo виден только при разрешающем флаге`() {
        composeRule.setContent { StitTheme { LoginScreen(state = LoginState(), onIntent = intents::add, onOpenSettings = {}) } }
        composeRule.onNodeWithTag(AuthTestTags.LOGIN_DEMO).assertDoesNotExist()
    }

    @Test
    fun `нажатие demo уходит интентом EnterDemo`() {
        val state = LoginState(demoAvailable = true)
        composeRule.setContent { StitTheme { LoginScreen(state = state, onIntent = intents::add, onOpenSettings = {}) } }
        composeRule.onNodeWithTag(AuthTestTags.LOGIN_DEMO).assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<LoginIntent>(LoginIntent.EnterDemo), intents)
    }

    @Test
    fun `во время отправки поля и кнопка выключены, настройки открываются`() {
        var settings = 0
        val state = LoginState(email = "demo@example.com", password = "secret", submitting = true)
        composeRule.setContent { StitTheme { LoginScreen(state = state, onIntent = intents::add, onOpenSettings = { settings++ }) } }
        composeRule.onNodeWithTag(AuthTestTags.LOGIN_SUBMIT).assertIsNotEnabled()
        composeRule.onNodeWithTag(AuthTestTags.LOGIN_EMAIL).assertIsNotEnabled()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.waitForIdle()
        assertEquals(1, settings)
    }
}
