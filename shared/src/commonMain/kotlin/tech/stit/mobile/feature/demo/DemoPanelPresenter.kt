package tech.stit.mobile.feature.demo

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.stit.mobile.core.lib.DispatcherProvider
import tech.stit.mobile.core.lib.presenterScope
import tech.stit.mobile.core.mock.DemoMode

// Debug-панель demo-режима (07 §3.2): сценарий и задержка сети; в prod-сборке недоступна, если DemoMode == null.
class DemoPanelPresenter internal constructor(
    private val demo: DemoMode?,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(DemoPanelState(available = demo != null))
    val state: StateFlow<DemoPanelState> = _state.asStateFlow()

    fun dispatch(intent: DemoPanelIntent) {
        val demo = demo ?: return
        when (intent) {
            DemoPanelIntent.Start -> _state.update { it.copy(scenario = demo.transport.scenario, latencyMs = demo.transport.latencyMs) }

            is DemoPanelIntent.SelectScenario -> scope.launch {
                demo.apply(intent.scenario)
                _state.update { it.copy(scenario = intent.scenario) }
            }

            is DemoPanelIntent.SetLatency -> {
                demo.transport.latencyMs = intent.latencyMs.coerceAtLeast(0)
                _state.update { it.copy(latencyMs = demo.transport.latencyMs) }
            }
        }
    }

    fun close() = scope.cancel()
}
