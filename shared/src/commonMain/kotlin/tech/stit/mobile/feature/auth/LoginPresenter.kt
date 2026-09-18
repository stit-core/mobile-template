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
import tech.stit.mobile.core.mock.DemoGate
import tech.stit.mobile.entity.session.SessionRepository

class LoginPresenter internal constructor(
    private val sessions: SessionRepository,
    private val demo: DemoGate?,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun dispatch(intent: LoginIntent) {
        when (intent) {
            LoginIntent.Start -> start()
            is LoginIntent.EmailChanged -> _state.update { it.copy(email = intent.value, error = null) }
            is LoginIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            LoginIntent.Submit -> submit()
            LoginIntent.EnterDemo -> enterDemo()
        }
    }

    fun close() = scope.cancel()

    private fun start() {
        val gate = demo ?: return
        scope.launch { gate.available.collect { available -> _state.update { it.copy(demoAvailable = available) } } }
    }

    private fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        scope.launch {
            attempt { sessions.login(current.email.trim(), current.password) }
                .onSuccess { _state.update { it.copy(submitting = false, password = "", signedIn = true) } }
                .onFailure { e -> _state.update { it.copy(submitting = false, error = e.toMessageKey()) } }
        }
    }

    // Форма блокируется на время переключения: демо-сессия поднимается тем же порядком, что и обычный вход.
    private fun enterDemo() {
        val gate = demo ?: return
        if (!_state.value.demoAvailable || _state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        scope.launch {
            attempt { gate.enter() }
            _state.update { it.copy(submitting = false, password = "") }
        }
    }
}
