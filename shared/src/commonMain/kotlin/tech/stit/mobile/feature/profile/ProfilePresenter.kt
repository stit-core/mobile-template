package tech.stit.mobile.feature.profile

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

// Профиль: чтение из SSOT UserRepository, оптимистичное имя, смена пароля, выход.
class ProfilePresenter internal constructor(
    private val users: UserRepository,
    private val sessions: SessionRepository,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    fun dispatch(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Start -> start()
            ProfileIntent.Refresh -> refresh()
            is ProfileIntent.NameChanged -> _state.update { it.copy(draftName = intent.value, error = null) }
            ProfileIntent.SaveName -> saveName()
            is ProfileIntent.ChangePassword -> changePassword(intent.oldPassword, intent.newPassword)
            ProfileIntent.Logout -> scope.launch { attempt { sessions.logout() } }
        }
    }

    fun close() = scope.cancel()

    private fun start() {
        scope.launch {
            users.me.collect { user -> _state.update { it.copy(user = user, draftName = user?.name.orEmpty()) } }
        }
        refresh()
    }

    private fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        scope.launch {
            attempt { users.refresh() }
                .onSuccess { _state.update { it.copy(loading = false) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toMessageKey()) } }
        }
    }

    private fun saveName() {
        val name = _state.value.draftName.trim()
        if (!_state.value.canSaveName) return
        _state.update { it.copy(saving = true, error = null) }
        scope.launch {
            attempt { users.updateName(name) }
                .onSuccess { _state.update { it.copy(saving = false) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.toMessageKey()) } }
        }
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        _state.update { it.copy(saving = true, error = null, passwordChanged = false) }
        scope.launch {
            attempt { users.changePassword(oldPassword, newPassword) }
                .onSuccess { _state.update { it.copy(saving = false, passwordChanged = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.toMessageKey()) } }
        }
    }
}
