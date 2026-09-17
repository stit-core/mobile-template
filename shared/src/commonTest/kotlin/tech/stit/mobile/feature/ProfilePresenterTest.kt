package tech.stit.mobile.feature

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.feature.profile.ProfileIntent
import tech.stit.mobile.testing.signIn
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfilePresenterTest {
    @Test
    fun startLoadsUserAndDraftName() = runTest {
        val container = testContainer()
        container.signIn()
        val presenter = container.profilePresenter()
        presenter.dispatch(ProfileIntent.Start)
        advanceUntilIdle()
        val state = presenter.state.value
        assertEquals("Демо Пользователь", state.user?.name)
        assertEquals("Демо Пользователь", state.draftName)
        assertFalse(state.loading)
        assertFalse(state.canSaveName)
        presenter.close()
    }

    @Test
    fun renameAndPasswordChangeSucceed() = runTest {
        val container = testContainer()
        container.signIn()
        val presenter = container.profilePresenter()
        presenter.dispatch(ProfileIntent.Start)
        advanceUntilIdle()
        presenter.dispatch(ProfileIntent.NameChanged("Новое имя"))
        assertTrue(presenter.state.value.canSaveName)
        presenter.dispatch(ProfileIntent.SaveName)
        advanceUntilIdle()
        assertEquals("Новое имя", presenter.state.value.user?.name)
        assertFalse(presenter.state.value.saving)

        presenter.dispatch(ProfileIntent.ChangePassword("old-pass", "new-pass"))
        advanceUntilIdle()
        assertTrue(presenter.state.value.passwordChanged)
        presenter.close()
    }

    @Test
    fun failuresSurfaceAsMessageKeys() = runTest {
        val container = testContainer()
        container.signIn()
        val presenter = container.profilePresenter()
        presenter.dispatch(ProfileIntent.Start)
        advanceUntilIdle()
        container.mockTransport!!.reset(MockScenario.Error)
        presenter.dispatch(ProfileIntent.Refresh)
        advanceUntilIdle()
        assertEquals(MessageKey("error.internal"), presenter.state.value.error)
        assertFalse(presenter.state.value.loading)
        presenter.close()
    }

    @Test
    fun logoutSignsOut() = runTest {
        val container = testContainer()
        container.signIn()
        val presenter = container.profilePresenter()
        presenter.dispatch(ProfileIntent.Logout)
        advanceUntilIdle()
        assertEquals(AuthState.SignedOut, container.sessionStore.authState.value)
        presenter.close()
    }
}
