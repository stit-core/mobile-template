package tech.stit.mobile.android.feature.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tech.stit.mobile.android.core.ui.DemoBanner
import tech.stit.mobile.android.core.ui.LoadingState
import tech.stit.mobile.android.core.ui.LockedState
import tech.stit.mobile.android.core.ui.Maintenance
import tech.stit.mobile.android.core.ui.OfflineBanner
import tech.stit.mobile.android.core.ui.UpdateRecommendedBanner
import tech.stit.mobile.android.core.ui.UpdateRequired
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.requirements.RequirementsStatus
import tech.stit.mobile.feature.requirements.RequirementsState
import tech.stit.mobile.feature.shell.ShellState

// Оболочка приложения (shell.md): блокирующие оверлеи requirements → замок → auth-gate → баннеры и вкладки вокруг контента.
@Composable
fun ShellScreen(
    shell: ShellState,
    requirements: RequirementsState,
    locked: Boolean,
    currentTab: ShellTab?,
    onTabSelect: (ShellTab) -> Unit,
    onUnlock: () -> Unit,
    onOpenStore: (String) -> Unit,
    onRetryRequirements: () -> Unit,
    onDismissRecommendation: () -> Unit,
    onExitDemo: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        // Вкладки — только при живой сессии: Settings открывается и до входа, но без корневой навигации.
        bottomBar = {
            if (currentTab != null &&
                shell.auth == AuthState.SignedIn
            ) {
                ShellNavigationBar(currentTab = currentTab, onTabSelect = onTabSelect)
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                requirements.status == RequirementsStatus.UpdateRequired ->
                    UpdateRequired(storeUrl = requirements.requirements.storeUrl, onOpenStore = onOpenStore)

                requirements.status == RequirementsStatus.Maintenance ->
                    Maintenance(until = requirements.requirements.maintenanceUntil, onRetry = onRetryRequirements)

                locked -> LockedState(onUnlock = onUnlock)

                shell.auth == AuthState.Unknown -> LoadingState()

                else -> Column {
                    if (shell.demo) DemoBanner(onExit = onExitDemo)
                    if (!shell.online) OfflineBanner()
                    if (requirements.showRecommendation) {
                        UpdateRecommendedBanner(
                            latestVersion = requirements.requirements.latestVersion,
                            onDismiss = onDismissRecommendation,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) { content() }
                }
            }
        }
    }
}

@Composable
private fun ShellNavigationBar(currentTab: ShellTab, onTabSelect: (ShellTab) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier) {
        ShellTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = { onTabSelect(tab) },
                icon = { Icon(painter = painterResource(tab.icon), contentDescription = null) },
                label = { Text(text = stringResource(tab.label)) },
            )
        }
    }
}
