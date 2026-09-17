package tech.stit.mobile.feature

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.config.ApiMode
import tech.stit.mobile.core.config.AppEnv
import tech.stit.mobile.core.lib.SettingsKeys
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.core.push.PushStatus
import tech.stit.mobile.feature.demo.DemoPanelIntent
import tech.stit.mobile.feature.push.PushOptInIntent
import tech.stit.mobile.feature.settings.AppLanguage
import tech.stit.mobile.feature.settings.SettingsIntent
import tech.stit.mobile.feature.settings.ThemeMode
import tech.stit.mobile.testing.FakeNotificationPermission
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.signIn
import tech.stit.mobile.testing.testConfig
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettingsPushDemoPresentersTest {
    @Test
    fun settingsRestorePersistAndExposeAboutInfo() = runTest {
        val platform = FakePlatformDeps()
        platform.settings.values[SettingsKeys.THEME] = "Dark"
        val container = testContainer(platform = platform)
        val presenter = container.settingsPresenter()
        assertEquals("1.0.0", presenter.state.value.version)
        assertEquals(1_000_000, presenter.state.value.build)
        assertEquals(AppEnv.Demo, presenter.state.value.env)

        presenter.dispatch(SettingsIntent.Start)
        container.requirements.fetch()
        advanceUntilIdle()
        assertEquals(ThemeMode.Dark, presenter.state.value.theme)
        assertEquals("https://app.example.com/account/delete", presenter.state.value.links.accountDeletion)

        presenter.dispatch(SettingsIntent.SetTheme(ThemeMode.Light))
        presenter.dispatch(SettingsIntent.SetLanguage(AppLanguage.En))
        advanceUntilIdle()
        assertEquals(ThemeMode.Light, presenter.state.value.theme)
        assertEquals(AppLanguage.En, presenter.state.value.language)
        assertEquals("Light", platform.settings.values[SettingsKeys.THEME])
        assertEquals("En", platform.settings.values[SettingsKeys.LANGUAGE])
        presenter.close()
    }

    @Test
    fun notificationToggleRegistersAndUnregisters() = runTest {
        val platform = FakePlatformDeps(notificationPermission = FakeNotificationPermission(granted = false, answer = true))
        val container = testContainer(platform = platform)
        container.signIn()
        val presenter = container.settingsPresenter()
        presenter.dispatch(SettingsIntent.SetNotifications(true))
        advanceUntilIdle()
        assertTrue(presenter.state.value.notificationsEnabled)
        assertNotNull(platform.settings.values[SettingsKeys.PUSH_REGISTRATION_PREFIX + "fcm"])

        presenter.dispatch(SettingsIntent.SetNotifications(false))
        advanceUntilIdle()
        assertFalse(presenter.state.value.notificationsEnabled)
        assertEquals("true", platform.settings.values[SettingsKeys.PUSH_DECLINED])
        presenter.close()
    }

    @Test
    fun pushOptInFollowsUserAnswer() = runTest {
        val platform = FakePlatformDeps(notificationPermission = FakeNotificationPermission(granted = false, answer = true))
        val container = testContainer(platform = platform)
        container.signIn()
        val presenter = container.pushOptInPresenter()
        presenter.dispatch(PushOptInIntent.Start)
        advanceUntilIdle()
        assertEquals(PushStatus.NotAsked, presenter.state.value.status)
        presenter.dispatch(PushOptInIntent.Accept)
        advanceUntilIdle()
        assertEquals(PushStatus.Granted, presenter.state.value.status)
        assertFalse(presenter.state.value.busy)
        presenter.dispatch(PushOptInIntent.Decline)
        advanceUntilIdle()
        assertEquals(PushStatus.Declined, presenter.state.value.status)
        presenter.close()
    }

    @Test
    fun demoPanelSwitchesScenarioAndLatency() = runTest {
        val platform = FakePlatformDeps()
        val container = testContainer(platform = platform)
        val presenter = container.demoPanelPresenter()
        presenter.dispatch(DemoPanelIntent.Start)
        assertTrue(presenter.state.value.available)
        assertEquals(MockScenario.Populated, presenter.state.value.scenario)

        presenter.dispatch(DemoPanelIntent.SelectScenario(MockScenario.Empty))
        advanceUntilIdle()
        assertEquals(MockScenario.Empty, presenter.state.value.scenario)
        assertEquals(MockScenario.Empty, container.mockTransport!!.scenario)
        assertEquals("empty", platform.settings.values[SettingsKeys.MOCK_SCENARIO])

        presenter.dispatch(DemoPanelIntent.SetLatency(-5))
        assertEquals(0, presenter.state.value.latencyMs)
        presenter.dispatch(DemoPanelIntent.SetLatency(250))
        assertEquals(250, container.mockTransport?.latencyMs)
        presenter.close()
    }

    @Test
    fun demoPanelIsUnavailableAgainstRealBackend() = runTest {
        val presenter = testContainer(config = testConfig(apiMode = ApiMode.Real)).demoPanelPresenter()
        presenter.dispatch(DemoPanelIntent.SelectScenario(MockScenario.Error))
        assertFalse(presenter.state.value.available)
        assertEquals(MockScenario.Populated, presenter.state.value.scenario)
        presenter.close()
    }
}
