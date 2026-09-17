package tech.stit.mobile.core.mock

import tech.stit.mobile.core.auth.SecureStore
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.lib.Settings
import tech.stit.mobile.core.lib.SettingsKeys

// Сценарий переживает перезапуск через Settings; authenticated сеет refresh-токен, чтобы стартовать залогиненным.
class DemoMode internal constructor(
    val transport: MockTransport,
    private val settings: Settings,
    private val secureStore: SecureStore,
    private val sessionStore: SessionStore,
) {
    // Вызывается первым шагом старта — до restore() сессии, иначе посев не будет прочитан.
    suspend fun restore() {
        val saved = settings.getString(SettingsKeys.MOCK_SCENARIO)?.let(MockScenario::parse) ?: MockScenario.Populated
        transport.reset(saved)
        seed(saved)
    }

    suspend fun apply(scenario: MockScenario) {
        transport.reset(scenario)
        settings.putString(SettingsKeys.MOCK_SCENARIO, scenario.name.lowercase())
        seed(scenario)
        if (scenario.seedsSession) sessionStore.restore()
    }

    private suspend fun seed(scenario: MockScenario) {
        if (scenario.seedsSession) {
            secureStore.write(SessionStore.REFRESH_TOKEN_KEY, MockTransport.SEED_REFRESH_TOKEN)
        } else if (secureStore.read(SessionStore.REFRESH_TOKEN_KEY) == MockTransport.SEED_REFRESH_TOKEN) {
            secureStore.remove(SessionStore.REFRESH_TOKEN_KEY)
        }
    }
}
