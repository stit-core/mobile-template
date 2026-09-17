package tech.stit.mobile.core.mock

import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.lib.SettingsKeys
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DemoModeTest {
    @Test
    fun restoreDefaultsToPopulatedWithoutSession() = runTest {
        val platform = FakePlatformDeps()
        val demo = testContainer(platform = platform).demoMode!!
        demo.restore()
        assertEquals(MockScenario.Populated, demo.transport.scenario)
        assertNull(platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY])
    }

    @Test
    fun authenticatedScenarioSeedsSessionAndSurvivesRestart() = runTest {
        val platform = FakePlatformDeps()
        val container = testContainer(platform = platform)
        container.demoMode!!.apply(MockScenario.Authenticated)
        assertEquals(AuthState.SignedIn, container.sessionStore.authState.value)
        assertEquals("authenticated", platform.settings.values[SettingsKeys.MOCK_SCENARIO])

        val restarted = testContainer(platform = platform)
        restarted.demoMode!!.restore()
        assertEquals(MockScenario.Authenticated, restarted.demoMode!!.transport.scenario)
        assertEquals(MockTransport.SEED_REFRESH_TOKEN, platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY])
        assertEquals(AuthState.SignedIn, restarted.sessionStore.restore())
    }

    @Test
    fun switchingAwayRemovesOnlyTheSeed() = runTest {
        val platform = FakePlatformDeps()
        val demo = testContainer(platform = platform).demoMode!!
        platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY] = MockTransport.SEED_REFRESH_TOKEN
        demo.apply(MockScenario.Empty)
        assertNull(platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY])

        platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY] = "mock-refresh-7"
        demo.apply(MockScenario.Populated)
        assertEquals("mock-refresh-7", platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY])
    }
}
