package tech.stit.mobile.android.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.entity.user.User
import tech.stit.mobile.feature.profile.ProfileState
import kotlin.time.Instant

private val previewUser = User(
    id = "u-1",
    email = "demo@example.com",
    name = "Демо Пользователь",
    status = "active",
    createdAt = Instant.parse("2026-01-10T09:00:00Z"),
    updatedAt = Instant.parse("2026-09-01T12:00:00Z"),
    version = 3,
)

@Preview(showBackground = true)
@Composable
private fun ProfilePopulatedPreview() {
    StitTheme {
        ProfileScreen(
            state = ProfileState(user = previewUser, draftName = previewUser.name),
            onIntent = {},
            onOpenSessions = {},
            onOpenDeleteAccount = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileErrorPreview() {
    StitTheme {
        ProfileScreen(state = ProfileState(error = MessageKey.NetworkError), onIntent = {}, onOpenSessions = {}, onOpenDeleteAccount = {})
    }
}
