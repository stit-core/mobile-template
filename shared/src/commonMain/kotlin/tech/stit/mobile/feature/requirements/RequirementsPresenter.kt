package tech.stit.mobile.feature.requirements

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.stit.mobile.core.lib.DispatcherProvider
import tech.stit.mobile.core.lib.presenterScope
import tech.stit.mobile.core.requirements.RequirementsClient

// GET /api/app/requirements на старте, при возврате из фона и по Retry с экрана обслуживания (07 §4.3).
class RequirementsPresenter internal constructor(
    private val client: RequirementsClient,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(RequirementsState())
    val state: StateFlow<RequirementsState> = _state.asStateFlow()

    fun dispatch(intent: RequirementsIntent) {
        when (intent) {
            RequirementsIntent.Start -> scope.launch {
                client.restore()
                _state.update { it.copy(requirements = client.current.value) }
                fetch(force = false)
            }

            RequirementsIntent.Foreground -> scope.launch { fetch(force = false) }

            RequirementsIntent.Retry -> scope.launch { fetch(force = true) }

            RequirementsIntent.DismissRecommendation -> _state.update { it.copy(recommendationDismissed = true) }
        }
    }

    fun close() = scope.cancel()

    private suspend fun fetch(force: Boolean) {
        val fresh = client.fetch(force)
        _state.update { it.copy(checked = true, requirements = fresh) }
    }
}
