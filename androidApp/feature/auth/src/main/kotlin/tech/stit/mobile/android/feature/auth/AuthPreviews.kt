package tech.stit.mobile.android.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.entity.session.Session
import tech.stit.mobile.feature.auth.DeleteAccountState
import tech.stit.mobile.feature.auth.LoginState
import tech.stit.mobile.feature.auth.SessionsState
import kotlin.time.Instant

@Preview(showBackground = true)
@Composable
private fun LoginEmptyPreview() {
    StitTheme { LoginScreen(state = LoginState(), onIntent = {}, onOpenSettings = {}) }
}

@Preview(showBackground = true)
@Composable
private fun LoginErrorPreview() {
    StitTheme {
        LoginScreen(
            state = LoginState(email = "demo@example.com", password = "x", error = MessageKey("error.auth_invalid_credentials")),
            onIntent = {},
            onOpenSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionsPopulatedPreview() {
    val session = Session(
        jti = "session-current",
        client = "android",
        deviceName = "Pixel 8",
        appVersion = "1.0.0+1000000",
        issuedAt = Instant.parse("2026-09-15T07:00:00Z"),
        expiresAt = Instant.parse("2026-12-14T07:00:00Z"),
        lastUsedAt = Instant.parse("2026-09-17T06:30:00Z"),
        current = true,
    )
    StitTheme { SessionsScreen(state = SessionsState(sessions = listOf(session)), onIntent = {}, onBack = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DeleteAccountConfirmedPreview() {
    StitTheme { DeleteAccountScreen(state = DeleteAccountState(confirmed = true), onIntent = {}, onBack = {}) }
}
