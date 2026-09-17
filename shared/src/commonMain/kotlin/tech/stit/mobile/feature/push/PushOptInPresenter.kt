package tech.stit.mobile.feature.push

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
import tech.stit.mobile.core.push.PushRegistrar
import tech.stit.mobile.core.push.PushStatus

// Пред-запрос разрешения на уведомления в момент ценности (07 §6): Accept → системный диалог → PUT /api/v1/devices.
class PushOptInPresenter internal constructor(
    private val pushRegistrar: PushRegistrar,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(PushOptInState())
    val state: StateFlow<PushOptInState> = _state.asStateFlow()

    fun dispatch(intent: PushOptInIntent) {
        when (intent) {
            PushOptInIntent.Start -> scope.launch { _state.update { it.copy(status = pushRegistrar.status()) } }

            PushOptInIntent.Accept -> accept()

            PushOptInIntent.Decline -> scope.launch {
                pushRegistrar.optOut()
                _state.update { it.copy(status = PushStatus.Declined) }
            }
        }
    }

    fun close() = scope.cancel()

    private fun accept() {
        _state.update { it.copy(busy = true, error = null) }
        scope.launch {
            attempt { pushRegistrar.optIn() }
                .onSuccess { granted ->
                    _state.update { it.copy(busy = false, status = if (granted) PushStatus.Granted else PushStatus.Declined) }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toMessageKey()) } }
        }
    }
}
