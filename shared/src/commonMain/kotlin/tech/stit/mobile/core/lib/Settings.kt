package tech.stit.mobile.core.lib

// Нечувствительные настройки (DataStore / NSUserDefaults); секреты — только SecureStore.
interface Settings {
    suspend fun getString(key: String): String?

    suspend fun putString(key: String, value: String)

    suspend fun remove(key: String)
}

// Реестр ключей: имена не расползаются по презентерам.
object SettingsKeys {
    const val THEME = "settings.theme"
    const val LANGUAGE = "settings.language"
    const val PUSH_DECLINED = "push.declined"
    const val PUSH_REGISTRATION_PREFIX = "push.registration."
    const val INSTALLATION_ID = "app.installation_id"
    const val MOCK_SCENARIO = "app.mock.scenario"

    // Единственный след demo-режима на реальной стороне: по нему старт после убитого в demo процесса чистит кеши.
    const val DEMO_ACTIVE = "app.demo.active"
}
