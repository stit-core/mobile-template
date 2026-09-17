package tech.stit.mobile.feature.demo

import tech.stit.mobile.core.mock.MockScenario

data class DemoPanelState(
    // false — сборка ходит в настоящий бэкенд (API_MODE=real), панель скрыта.
    val available: Boolean = false,
    val scenario: MockScenario = MockScenario.Populated,
    val latencyMs: Long = 0,
    val scenarios: List<MockScenario> = MockScenario.entries,
)
