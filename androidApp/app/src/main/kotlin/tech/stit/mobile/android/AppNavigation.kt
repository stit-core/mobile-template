package tech.stit.mobile.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import tech.stit.mobile.android.core.navigation.Destination
import tech.stit.mobile.android.core.navigation.current
import tech.stit.mobile.android.core.navigation.navigateTo
import tech.stit.mobile.android.core.navigation.pop
import tech.stit.mobile.android.core.navigation.resetTo
import tech.stit.mobile.android.core.navigation.toDestination
import tech.stit.mobile.android.feature.auth.authEntries
import tech.stit.mobile.android.feature.profile.profileEntries
import tech.stit.mobile.android.feature.settings.settingsEntries
import tech.stit.mobile.android.feature.shell.ShellRoute
import tech.stit.mobile.android.feature.shell.ShellTab
import tech.stit.mobile.android.feature.todos.todosEntries
import tech.stit.mobile.core.auth.AuthState

// Единственное место, где фичи знают друг о друге: app — медиатор, feature-модули видят только Destination и колбэки.
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Destination.TodoList)
    val uriHandler = LocalUriHandler.current
    ShellRoute(
        currentTab = backStack.current?.tab(),
        onTabSelect = { backStack.resetTo(it.destination()) },
        onAuthChange = { auth ->
            when (auth) {
                AuthState.SignedOut -> backStack.resetTo(Destination.Login)
                AuthState.SignedIn -> if (backStack.first() == Destination.Login) backStack.resetTo(Destination.TodoList)
                AuthState.Unknown -> Unit
            }
        },
        onRoute = { route -> backStack.navigateTo(route.toDestination()) },
        modifier = modifier,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.pop() },
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
            entryProvider = entryProvider {
                authEntries(onOpenSettings = { backStack.navigateTo(Destination.Settings) }, onBack = { backStack.pop() })
                todosEntries(onOpenTodo = { backStack.navigateTo(Destination.TodoDetail(it)) }, onBack = { backStack.pop() })
                profileEntries(
                    onOpenSessions = { backStack.navigateTo(Destination.Sessions) },
                    onOpenDeleteAccount = { backStack.navigateTo(Destination.DeleteAccount) },
                )
                settingsEntries(
                    onOpenNotifications = { backStack.navigateTo(Destination.PushOptIn) },
                    onOpenLink = uriHandler::openUri,
                    onBack = { backStack.pop() },
                )
            },
        )
    }
}

private fun ShellTab.destination(): Destination = when (this) {
    ShellTab.Todos -> Destination.TodoList
    ShellTab.Profile -> Destination.Profile
    ShellTab.Settings -> Destination.Settings
}

private fun Destination.tab(): ShellTab? = when (this) {
    Destination.TodoList -> ShellTab.Todos
    Destination.Profile -> ShellTab.Profile
    Destination.Settings -> ShellTab.Settings
    else -> null
}
