package tech.stit.mobile.core.mock

// Сценарии mock-режима (07 §3): те же имена, что у SPA; каталог фикстур — mock/fixtures/<имя в нижнем регистре>/.
enum class MockScenario {
    Empty,
    Populated,
    Authenticated,
    Error,
    ;

    val fixtureDir: String get() = name.lowercase()

    // authenticated стартует с посеянным refresh-токеном: экран входа пропускается (скриншоты, тесты).
    val seedsSession: Boolean get() = this == Authenticated

    companion object {
        fun parse(raw: String): MockScenario? = entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}
