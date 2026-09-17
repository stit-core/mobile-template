package tech.stit.mobile.android.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.core.config.AppEnv
import tech.stit.mobile.core.push.PushStatus
import tech.stit.mobile.core.requirements.AppLinks
import tech.stit.mobile.feature.demo.DemoPanelState
import tech.stit.mobile.feature.push.PushOptInState
import tech.stit.mobile.feature.settings.SettingsState

private val previewState = SettingsState(
    version = "1.0.0",
    build = 1_000_000,
    env = AppEnv.Demo,
    links = AppLinks(
        privacyPolicy = "https://app.example.com/privacy",
        accountDeletion = null,
        support = "https://app.example.com/support",
    ),
)

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    StitTheme {
        SettingsScreen(
            state = previewState,
            demo = DemoPanelState(available = true),
            onIntent = {},
            onDemoIntent = {},
            onOpenNotifications = {},
            onOpenLink = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PushOptInPreview() {
    StitTheme { PushOptInScreen(state = PushOptInState(status = PushStatus.NotAsked), onIntent = {}, onBack = {}) }
}
