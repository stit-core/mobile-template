package tech.stit.mobile.feature.auth

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.stit.mobile.core.api.toMessageKey
import tech.stit.mobile.core.lib.DispatcherProvider
import tech.stit.mobile.core.lib.attempt
import tech.stit.mobile.core.lib.presenterScope
import tech.stit.mobile.entity.device.DeviceRepository
import tech.stit.mobile.entity.session.SessionRepository

// Активные сессии и push-устройства пользователя: отзыв по одному или «выйти со всех устройств».
class SessionsPresenter internal constructor(
    private val sessions: SessionRepository,
    private val devices: DeviceRepository,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(SessionsState())
    val state: StateFlow<SessionsState> = _state.asStateFlow()

    fun dispatch(intent: SessionsIntent) {
        when (intent) {
            SessionsIntent.Refresh -> refresh()

            is SessionsIntent.RevokeSession -> mutate { sessions.revoke(intent.jti) }

            is SessionsIntent.UnregisterDevice -> mutate { devices.unregister(intent.id) }

            SessionsIntent.RevokeAll -> scope.launch {
                attempt { sessions.revokeAll() }
                    .onSuccess { _state.update { it.copy(signedOut = true) } }
                    .onFailure { e -> _state.update { it.copy(error = e.toMessageKey()) } }
            }
        }
    }

    fun close() = scope.cancel()

    private fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        scope.launch {
            attempt { sessions.sessions() to devices.list() }
                .onSuccess { (list, devs) -> _state.update { it.copy(loading = false, sessions = list, devices = devs) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toMessageKey()) } }
        }
    }

    private fun mutate(action: suspend () -> Unit) {
        scope.launch {
            attempt { action() }
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(error = e.toMessageKey()) } }
        }
    }
}
