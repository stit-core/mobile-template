package tech.stit.mobile.android.feature.shell

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.deeplink.Route
import tech.stit.mobile.feature.requirements.RequirementsIntent
import tech.stit.mobile.feature.shell.ShellIntent

// Stateful-обёртка оболочки: презентеры shell и requirements живут на уровне Activity, навигацию выполняет app.
@Composable
fun ShellRoute(
    currentTab: ShellTab?,
    onTabSelect: (ShellTab) -> Unit,
    onAuthChange: (AuthState) -> Unit,
    onRoute: (Route) -> Unit,
    modifier: Modifier = Modifier,
    shellViewModel: ShellViewModel = hiltViewModel(),
    requirementsViewModel: RequirementsViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val shell by shellViewModel.state.collectAsStateWithLifecycle()
    val requirements by requirementsViewModel.state.collectAsStateWithLifecycle()
    val unlocked by shellViewModel.unlocked.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val uriHandler = LocalUriHandler.current

    val currentOnAuthChange by rememberUpdatedState(onAuthChange)
    val currentOnRoute by rememberUpdatedState(onRoute)

    LaunchedEffect(shell.auth) { currentOnAuthChange(shell.auth) }
    // Одноразовый маршрут: app переходит и презентер получает RouteConsumed — повторная навигация исключена.
    LaunchedEffect(shell.route) {
        shell.route?.let { route ->
            currentOnRoute(route)
            shellViewModel.dispatch(ShellIntent.RouteConsumed)
        }
    }
    LifecycleResumeEffect(Unit) {
        requirementsViewModel.dispatch(RequirementsIntent.Foreground)
        onPauseOrDispose {}
    }

    ShellScreen(
        shell = shell,
        requirements = requirements,
        locked = !unlocked,
        currentTab = currentTab,
        onTabSelect = onTabSelect,
        onUnlock = { activity?.let(shellViewModel::unlock) },
        onOpenStore = uriHandler::openUri,
        onRetryRequirements = { requirementsViewModel.dispatch(RequirementsIntent.Retry) },
        onDismissRecommendation = { requirementsViewModel.dispatch(RequirementsIntent.DismissRecommendation) },
        modifier = modifier,
        content = content,
    )
}
