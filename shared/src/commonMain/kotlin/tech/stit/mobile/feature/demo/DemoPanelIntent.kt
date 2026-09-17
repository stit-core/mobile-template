package tech.stit.mobile.feature.demo

import tech.stit.mobile.core.mock.MockScenario

sealed class DemoPanelIntent {
    data object Start : DemoPanelIntent()

    data class SelectScenario(val scenario: MockScenario) : DemoPanelIntent()

    data class SetLatency(val latencyMs: Long) : DemoPanelIntent()
}
