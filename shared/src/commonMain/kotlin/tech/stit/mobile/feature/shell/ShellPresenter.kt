package tech.stit.mobile.feature.shell

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.deeplink.DeepLinkParser
import tech.stit.mobile.core.lib.ConnectivityMonitor
import tech.stit.mobile.core.lib.DispatcherProvider
import tech.stit.mobile.core.lib.attempt
import tech.stit.mobile.core.lib.presenterScope
import tech.stit.mobile.core.mock.DemoMode
import tech.stit.mobile.core.observability.Logger
import tech.stit.mobile.core.push.PushRegistrar
import tech.stit.mobile.entity.todo.TodoRepository
import tech.stit.mobile.entity.user.UserRepository

// Старт приложения одним сценарием: demo-режим → кеши → сессия; затем auth-gate, offline и pending route.
class ShellPresenter internal constructor(
    private val demo: DemoMode?,
    private val sessionStore: SessionStore,
    private val users: UserRepository,
    private val todos: TodoRepository,
    private val pushRegistrar: PushRegistrar,
    private val deepLinks: DeepLinkParser,
    private val connectivity: ConnectivityMonitor,
    private val logger: Logger,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(ShellState())
    val state: StateFlow<ShellState> = _state.asStateFlow()

    fun dispatch(intent: ShellIntent) {
        when (intent) {
            ShellIntent.Start -> start()
            is ShellIntent.OpenLink -> openLink(intent.url)
            ShellIntent.RouteConsumed -> _state.update { it.copy(route = null) }
        }
    }

    fun close() = scope.cancel()

    private fun start() {
        scope.launch { connectivity.online.collect { online -> _state.update { it.copy(online = online) } } }
        scope.launch { sessionStore.authState.collect(::onAuthChanged) }
        scope.launch {
            demo?.restore()
            users.restore()
            todos.restore()
            sessionStore.restore()
        }
    }

    private suspend fun onAuthChanged(auth: AuthState) {
        _state.update { current ->
            val pending = current.pendingRoute?.takeIf { auth == AuthState.SignedIn }
            current.copy(auth = auth, route = pending ?: current.route, pendingRoute = current.pendingRoute.takeIf { pending == null })
        }
        if (auth == AuthState.SignedIn) {
            attempt { pushRegistrar.register() }.onFailure { logger.warn("push-регистрация на старте не удалась", it) }
        }
    }

    private fun openLink(url: String) {
        val route = deepLinks.parse(url) ?: return
        _state.update {
            if (route.requiresAuth && it.auth != AuthState.SignedIn) it.copy(pendingRoute = route) else it.copy(route = route)
        }
    }
}
