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
import tech.stit.mobile.entity.user.UserRepository

// Удаление аккаунта из приложения — обязательный экран (App Store 5.1.1(v)): подтверждение → DELETE → локальный выход.
class DeleteAccountPresenter internal constructor(
    private val users: UserRepository,
    private val sessions: SessionRepository,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(DeleteAccountState())
    val state: StateFlow<DeleteAccountState> = _state.asStateFlow()

    fun dispatch(intent: DeleteAccountIntent) {
        when (intent) {
            is DeleteAccountIntent.ConfirmChanged -> _state.update { it.copy(confirmed = intent.confirmed, error = null) }
            DeleteAccountIntent.Delete -> delete()
        }
    }

    fun close() = scope.cancel()

    private fun delete() {
        if (!_state.value.canDelete) return
        _state.update { it.copy(deleting = true, error = null) }
        scope.launch {
            attempt {
                users.deleteAccount()
                sessions.logout()
            }
                .onSuccess { _state.update { it.copy(deleting = false, deleted = true) } }
                .onFailure { e -> _state.update { it.copy(deleting = false, error = e.toMessageKey()) } }
        }
    }
}
