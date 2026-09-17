package tech.stit.mobile.feature.shell

import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.deeplink.Route

data class ShellState(
    val auth: AuthState = AuthState.Unknown,
    val online: Boolean = true,
    // Одноразовая навигация: UI переходит и шлёт RouteConsumed.
    val route: Route? = null,
    // Deep link, который ждёт входа (07 §5.3).
    val pendingRoute: Route? = null,
)
