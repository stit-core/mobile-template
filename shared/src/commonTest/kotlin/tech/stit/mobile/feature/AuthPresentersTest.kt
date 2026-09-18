package tech.stit.mobile.feature

import app.cash.turbine.test
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.feature.auth.DeleteAccountIntent
import tech.stit.mobile.feature.auth.LoginIntent
import tech.stit.mobile.feature.auth.LoginState
import tech.stit.mobile.feature.auth.SessionsIntent
import tech.stit.mobile.testing.prodContainer
import tech.stit.mobile.testing.signIn
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthPresentersTest {
    @Test
    fun loginGoesThroughSubmittingToSignedIn() = runTest {
        val container = testContainer()
        val presenter = container.loginPresenter()
        presenter.state.test {
            assertEquals(LoginState(), awaitItem())
            presenter.dispatch(LoginIntent.Submit)
            presenter.dispatch(LoginIntent.EmailChanged("demo@example.com"))
            assertFalse(awaitItem().canSubmit)
            presenter.dispatch(LoginIntent.PasswordChanged("secret"))
            assertTrue(awaitItem().canSubmit)
            presenter.dispatch(LoginIntent.Submit)
            assertTrue(awaitItem().submitting)
            val done = awaitItem()
            assertTrue(done.signedIn)
            assertEquals("", done.password)
            assertEquals(AuthState.SignedIn, container.sessionStore.authState.value)
        }
        presenter.close()
    }

    @Test
    fun loginFailureShowsErrorKeyAndStaysOnScreen() = runTest {
        val presenter = testContainer(MockScenario.Error).loginPresenter()
        presenter.dispatch(LoginIntent.EmailChanged("demo@example.com"))
        presenter.dispatch(LoginIntent.PasswordChanged("secret"))
        presenter.dispatch(LoginIntent.Submit)
        advanceUntilIdle()
        val state = presenter.state.value
        assertEquals(MessageKey("error.internal"), state.error)
        assertFalse(state.submitting)
        assertFalse(state.signedIn)
        presenter.dispatch(LoginIntent.EmailChanged("x@y"))
        assertNull(presenter.state.value.error)
        presenter.close()
    }

    // Обычная сборка: вход в demo на экране логина открывает только флаг requirements.demo_login.
    @Test
    fun loginShowsDemoEntryOnlyWithTheFlag() = runTest {
        val container = prodContainer()
        val presenter = container.loginPresenter()
        presenter.dispatch(LoginIntent.Start)
        advanceUntilIdle()
        assertFalse(presenter.state.value.demoAvailable)

        container.requirements.fetch()
        advanceUntilIdle()
        assertTrue(presenter.state.value.demoAvailable)

        presenter.dispatch(LoginIntent.EnterDemo)
        advanceUntilIdle()
        assertTrue(requireNotNull(container.demoGate).active.value)
        assertEquals(AuthState.SignedIn, container.sessionStore.authState.value)
        assertFalse(presenter.state.value.submitting)
        presenter.close()
    }

    @Test
    fun loginHidesDemoEntryInTheDemoBuild() = runTest {
        val presenter = testContainer().loginPresenter()
        presenter.dispatch(LoginIntent.Start)
        advanceUntilIdle()
        assertFalse(presenter.state.value.demoAvailable)
        presenter.dispatch(LoginIntent.EnterDemo)
        advanceUntilIdle()
        assertFalse(presenter.state.value.submitting)
        presenter.close()
    }

    @Test
    fun sessionsScreenListsRevokesAndSignsOutEverywhere() = runTest {
        val container = testContainer()
        container.signIn()
        val presenter = container.sessionsPresenter()
        presenter.dispatch(SessionsIntent.Refresh)
        advanceUntilIdle()
        assertEquals(2, presenter.state.value.sessions.size)
        assertEquals(1, presenter.state.value.devices.size)

        presenter.dispatch(SessionsIntent.RevokeSession("session-web"))
        advanceUntilIdle()
        assertEquals(listOf("session-current"), presenter.state.value.sessions.map { it.jti })
        presenter.dispatch(SessionsIntent.UnregisterDevice("dev-1"))
        advanceUntilIdle()
        assertTrue(presenter.state.value.devices.isEmpty())

        presenter.dispatch(SessionsIntent.RevokeAll)
        advanceUntilIdle()
        assertTrue(presenter.state.value.signedOut)
        assertEquals(AuthState.SignedOut, container.sessionStore.authState.value)
        presenter.close()
    }

    @Test
    fun sessionsScreenReportsErrors() = runTest {
        val container = testContainer()
        container.signIn()
        container.mockTransport!!.reset(MockScenario.Error)
        val presenter = container.sessionsPresenter()
        presenter.dispatch(SessionsIntent.Refresh)
        advanceUntilIdle()
        assertEquals(MessageKey("error.internal"), presenter.state.value.error)
        assertFalse(presenter.state.value.loading)
        presenter.close()
    }

    @Test
    fun deleteAccountNeedsConfirmationThenSignsOut() = runTest {
        val container = testContainer()
        container.signIn()
        container.userRepository.refresh()
        val presenter = container.deleteAccountPresenter()
        presenter.dispatch(DeleteAccountIntent.Delete)
        advanceUntilIdle()
        assertFalse(presenter.state.value.deleted)

        presenter.dispatch(DeleteAccountIntent.ConfirmChanged(true))
        assertTrue(presenter.state.value.canDelete)
        presenter.dispatch(DeleteAccountIntent.Delete)
        advanceUntilIdle()
        assertTrue(presenter.state.value.deleted)
        assertEquals(AuthState.SignedOut, container.sessionStore.authState.value)
        presenter.close()
    }

    @Test
    fun deleteAccountFailureKeepsTheSession() = runTest {
        val container = testContainer()
        container.signIn()
        container.userRepository.refresh()
        container.mockTransport!!.reset(MockScenario.Error)
        val presenter = container.deleteAccountPresenter()
        presenter.dispatch(DeleteAccountIntent.ConfirmChanged(true))
        presenter.dispatch(DeleteAccountIntent.Delete)
        advanceUntilIdle()
        assertEquals(MessageKey("error.internal"), presenter.state.value.error)
        assertEquals(AuthState.SignedIn, container.sessionStore.authState.value)
        presenter.close()
    }
}
