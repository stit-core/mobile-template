package tech.stit.mobile.android.feature.settings

import org.junit.Test
import tech.stit.mobile.android.core.testing.ScreenshotTest
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.core.push.PushStatus
import tech.stit.mobile.feature.demo.DemoPanelState
import tech.stit.mobile.feature.push.PushOptInState

class SettingsScreenshotTest : ScreenshotTest() {
    @Test
    fun `настройки`() = capture("settings_populated") { Settings(DemoPanelState()) }

    @Test
    fun `настройки с demo-панелью`() = capture("settings_demo") { Settings(DemoPanelState(available = true, latencyMs = 250)) }

    @Test
    fun `настройки в тёмной теме`() = capture("settings_populated_dark", darkTheme = true) { Settings(DemoPanelState()) }

    @Test
    fun `уведомления — не спрашивали`() = capture("push_opt_in_not_asked") { PushOptIn(PushOptInState()) }

    @Test
    fun `уведомления — включены`() = capture("push_opt_in_granted") { PushOptIn(PushOptInState(status = PushStatus.Granted)) }

    @Test
    fun `уведомления — отключены`() = capture("push_opt_in_declined") { PushOptIn(PushOptInState(status = PushStatus.Declined)) }

    @Test
    fun `уведомления — запрос идёт`() = capture("push_opt_in_busy") { PushOptIn(PushOptInState(busy = true)) }

    @Test
    fun `уведомления — ошибка`() = capture("push_opt_in_error") { PushOptIn(PushOptInState(error = MessageKey.NetworkError)) }
}

@androidx.compose.runtime.Composable
private fun Settings(demo: DemoPanelState) = SettingsScreen(
    state = demoSettings,
    demo = demo,
    onIntent = {},
    onDemoIntent = {},
    onOpenNotifications = {},
    onOpenLink = {},
)

@androidx.compose.runtime.Composable
private fun PushOptIn(state: PushOptInState) = PushOptInScreen(state = state, onIntent = {}, onBack = {})
