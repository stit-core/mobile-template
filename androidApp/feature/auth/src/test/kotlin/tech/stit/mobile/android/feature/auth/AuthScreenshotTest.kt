package tech.stit.mobile.android.feature.auth

import org.junit.Test
import tech.stit.mobile.android.core.testing.ScreenshotTest
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.feature.auth.DeleteAccountState
import tech.stit.mobile.feature.auth.LoginState
import tech.stit.mobile.feature.auth.SessionsState

class AuthScreenshotTest : ScreenshotTest() {
    private val filled = LoginState(email = "demo@example.com", password = "secret")

    @Test
    fun `вход — пустая форма`() = capture("login_empty") { LoginScreen(state = LoginState(), onIntent = {}, onOpenSettings = {}) }

    @Test
    fun `вход — заполнена`() = capture("login_filled") { LoginScreen(state = filled, onIntent = {}, onOpenSettings = {}) }

    @Test
    fun `вход — отправка`() = capture("login_submitting") {
        LoginScreen(state = filled.copy(submitting = true), onIntent = {}, onOpenSettings = {})
    }

    @Test
    fun `вход — ошибка`() = capture("login_error") {
        LoginScreen(state = filled.copy(error = MessageKey("error.auth_invalid_credentials")), onIntent = {}, onOpenSettings = {})
    }

    @Test
    fun `вход — тёмная тема`() = capture("login_filled_dark", darkTheme = true) {
        LoginScreen(state = filled, onIntent = {}, onOpenSettings = {})
    }

    @Test
    fun `сессии — загрузка`() = capture("sessions_loading") {
        SessionsScreen(state = SessionsState(loading = true), onIntent = {}, onBack = {})
    }

    @Test
    fun `сессии — список`() = capture("sessions_populated") {
        SessionsScreen(state = SessionsState(sessions = listOf(currentSession, webSession), devices = listOf(fcmDevice)), onIntent = {
        }, onBack = {})
    }

    @Test
    fun `сессии — пусто`() = capture("sessions_empty") { SessionsScreen(state = SessionsState(), onIntent = {}, onBack = {}) }

    @Test
    fun `сессии — ошибка`() = capture("sessions_error") {
        SessionsScreen(state = SessionsState(error = MessageKey.NetworkError), onIntent = {}, onBack = {})
    }

    @Test
    fun `удаление — начало`() = capture("delete_account_initial") {
        DeleteAccountScreen(state = DeleteAccountState(), onIntent = {}, onBack = {})
    }

    @Test
    fun `удаление — подтверждено`() = capture("delete_account_confirmed") {
        DeleteAccountScreen(state = DeleteAccountState(confirmed = true), onIntent = {}, onBack = {})
    }

    @Test
    fun `удаление — в процессе`() = capture("delete_account_deleting") {
        DeleteAccountScreen(state = DeleteAccountState(confirmed = true, deleting = true), onIntent = {}, onBack = {})
    }

    @Test
    fun `удаление — ошибка`() = capture("delete_account_error") {
        DeleteAccountScreen(state = DeleteAccountState(confirmed = true, error = MessageKey("error.internal")), onIntent = {}, onBack = {})
    }
}
