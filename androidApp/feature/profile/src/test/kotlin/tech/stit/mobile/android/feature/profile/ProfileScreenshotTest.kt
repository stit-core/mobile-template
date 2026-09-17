package tech.stit.mobile.android.feature.profile

import org.junit.Test
import tech.stit.mobile.android.core.testing.ScreenshotTest
import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.feature.profile.ProfileState

class ProfileScreenshotTest : ScreenshotTest() {
    private val populated = ProfileState(user = demoUser, draftName = demoUser.name)

    @Test
    fun `загрузка`() = capture("profile_loading") { Profile(ProfileState(loading = true)) }

    @Test
    fun `профиль`() = capture("profile_populated") { Profile(populated) }

    @Test
    fun `профиль в тёмной теме`() = capture("profile_populated_dark", darkTheme = true) { Profile(populated) }

    @Test
    fun `ошибка загрузки`() = capture("profile_error") { Profile(ProfileState(error = MessageKey.NetworkError)) }

    @Test
    fun `сохранение имени`() = capture("profile_saving") { Profile(populated.copy(draftName = "Новое имя", saving = true)) }

    @Test
    fun `пароль изменён, ошибка сохранения`() = capture("profile_password_changed") {
        Profile(populated.copy(passwordChanged = true, error = MessageKey("error.validation")))
    }
}

@androidx.compose.runtime.Composable
private fun Profile(state: ProfileState) {
    ProfileScreen(state = state, onIntent = {}, onOpenSessions = {}, onOpenDeleteAccount = {})
}
