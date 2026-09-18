package tech.stit.mobile.android.feature.shell

import org.junit.Test
import tech.stit.mobile.android.core.testing.ScreenshotTest
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.requirements.AppRequirements
import tech.stit.mobile.core.requirements.RequirementsStatus
import tech.stit.mobile.feature.requirements.RequirementsState
import tech.stit.mobile.feature.shell.ShellState

class ShellScreenshotTest : ScreenshotTest() {
    @Test
    fun `сессия неизвестна`() = capture("shell_loading") { Shell(shell = ShellState(auth = AuthState.Unknown)) }

    @Test
    fun `без сессии — без вкладок`() = capture("shell_signed_out") { Shell(shell = ShellState(auth = AuthState.SignedOut)) }

    @Test
    fun `вкладки и контент`() = capture("shell_signed_in") { Shell(shell = ShellState(auth = AuthState.SignedIn)) }

    @Test
    fun `офлайн-баннер`() = capture("shell_offline") { Shell(shell = ShellState(auth = AuthState.SignedIn, online = false)) }

    @Test
    fun `нужно обновление`() = capture("shell_update_required") {
        Shell(shell = ShellState(auth = AuthState.SignedIn), requirements = requirements(RequirementsStatus.UpdateRequired))
    }

    @Test
    fun `технические работы`() = capture("shell_maintenance") {
        Shell(shell = ShellState(auth = AuthState.SignedIn), requirements = requirements(RequirementsStatus.Maintenance))
    }

    @Test
    fun `рекомендация обновиться`() = capture("shell_update_recommended") {
        Shell(shell = ShellState(auth = AuthState.SignedIn), requirements = requirements(RequirementsStatus.UpdateRecommended))
    }

    @Test
    fun `демо-баннер`() = capture("shell_demo") { Shell(shell = ShellState(auth = AuthState.SignedIn, demo = true)) }

    @Test
    fun `биометрический замок`() = capture("shell_locked") { Shell(shell = ShellState(auth = AuthState.SignedIn), locked = true) }

    @Test
    fun `вкладки в тёмной теме`() = capture("shell_signed_in_dark", darkTheme = true) {
        Shell(shell = ShellState(auth = AuthState.SignedIn))
    }

    private fun requirements(status: RequirementsStatus) = RequirementsState(
        checked = true,
        requirements = AppRequirements.Default.copy(status = status, latestVersion = "1.2.0", storeUrl = "https://store.example"),
    )
}
