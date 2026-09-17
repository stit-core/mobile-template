package tech.stit.mobile.android.feature.shell

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.requirements.AppRequirements
import tech.stit.mobile.core.requirements.RequirementsStatus
import tech.stit.mobile.feature.requirements.RequirementsState
import tech.stit.mobile.feature.shell.ShellState

@Preview(showBackground = true)
@Composable
private fun ShellSignedInPreview() {
    StitTheme { Shell(shell = ShellState(auth = AuthState.SignedIn)) }
}

@Preview(showBackground = true)
@Composable
private fun ShellOfflinePreview() {
    StitTheme { Shell(shell = ShellState(auth = AuthState.SignedIn, online = false)) }
}

@Preview(showBackground = true)
@Composable
private fun ShellUpdateRequiredPreview() {
    StitTheme {
        Shell(
            shell = ShellState(auth = AuthState.SignedIn),
            requirements = RequirementsState(
                checked = true,
                requirements = AppRequirements.Default.copy(status = RequirementsStatus.UpdateRequired),
            ),
        )
    }
}

@Composable
private fun Shell(shell: ShellState, requirements: RequirementsState = RequirementsState(), locked: Boolean = false) {
    ShellScreen(
        shell = shell,
        requirements = requirements,
        locked = locked,
        currentTab = ShellTab.Todos,
        onTabSelect = {},
        onUnlock = {},
        onOpenStore = {},
        onRetryRequirements = {},
        onDismissRecommendation = {},
    ) { Text(text = "content") }
}
