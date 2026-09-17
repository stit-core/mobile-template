package tech.stit.mobile.feature

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.deeplink.Route
import tech.stit.mobile.core.lib.SettingsKeys
import tech.stit.mobile.feature.shell.ShellIntent
import tech.stit.mobile.feature.shell.ShellPresenter
import tech.stit.mobile.testing.FakeNotificationPermission
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.signIn
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ShellPresenterTest {
    @Test
    fun startWithoutSessionEndsSignedOut() = runTest {
        val container = testContainer()
        val presenter = container.shellPresenter()
        assertEquals(AuthState.Unknown, presenter.state.value.auth)
        presenter.dispatch(ShellIntent.Start)
        advanceUntilIdle()
        assertEquals(AuthState.SignedOut, presenter.state.value.auth)
        presenter.close()
    }

    @Test
    fun savedAuthenticatedScenarioStartsSignedInAndRegistersPush() = runTest {
        val platform = FakePlatformDeps(notificationPermission = FakeNotificationPermission(granted = true))
        platform.settings.values[SettingsKeys.MOCK_SCENARIO] = "authenticated"
        val presenter = testContainer(platform = platform).shellPresenter()
        presenter.dispatch(ShellIntent.Start)
        advanceUntilIdle()
        assertEquals(AuthState.SignedIn, presenter.state.value.auth)
        assertNotNull(platform.settings.values[SettingsKeys.PUSH_REGISTRATION_PREFIX + "fcm"])
        presenter.close()
    }

    @Test
    fun deepLinkWaitsForSignInThenNavigatesOnce() = runTest {
        val container = testContainer()
        val presenter = container.shellPresenter()
        presenter.dispatch(ShellIntent.Start)
        advanceUntilIdle()
        presenter.dispatch(ShellIntent.OpenLink("https://app.demo.invalid/todos/todo-1"))
        assertEquals(Route.TodoDetail("todo-1"), presenter.state.value.pendingRoute)
        assertNull(presenter.state.value.route)

        container.signIn()
        advanceUntilIdle()
        assertEquals(Route.TodoDetail("todo-1"), presenter.state.value.route)
        assertNull(presenter.state.value.pendingRoute)
        presenter.dispatch(ShellIntent.RouteConsumed)
        assertNull(presenter.state.value.route)
        presenter.close()
    }

    @Test
    fun openLinksBeforeSignInAndUnknownLinksBehaveAsExpected() = runTest {
        val presenter = testContainer().shellPresenter()
        presenter.dispatch(ShellIntent.Start)
        advanceUntilIdle()
        presenter.dispatch(ShellIntent.OpenLink("stit://settings"))
        assertEquals(Route.Settings, presenter.state.value.route)
        presenter.dispatch(ShellIntent.OpenLink("https://evil.invalid/todos/1"))
        assertEquals(Route.Settings, presenter.state.value.route)
        assertNull(presenter.state.value.pendingRoute)
        presenter.close()
    }

    @Test
    fun onlineFlagFollowsConnectivity() = runTest {
        val platform = FakePlatformDeps()
        val presenter = testContainer(platform = platform).shellPresenter()
        presenter.dispatch(ShellIntent.Start)
        advanceUntilIdle()
        platform.connectivity.online.value = false
        advanceUntilIdle()
        assertFalse(presenter.state.value.online)
        presenter.close()
    }
}
