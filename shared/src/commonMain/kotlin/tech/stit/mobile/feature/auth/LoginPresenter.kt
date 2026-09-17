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
import tech.stit.mobile.entity.session.SessionRepository

class LoginPresenter internal constructor(
    private val sessions: SessionRepository,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun dispatch(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> _state.update { it.copy(email = intent.value, error = null) }
            is LoginIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            LoginIntent.Submit -> submit()
        }
    }

    fun close() = scope.cancel()

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
}
