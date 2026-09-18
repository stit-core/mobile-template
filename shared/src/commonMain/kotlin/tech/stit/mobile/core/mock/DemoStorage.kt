package tech.stit.mobile.core.mock

import tech.stit.mobile.core.auth.SecureStore
import tech.stit.mobile.core.lib.Settings

// Хранилища demo-сессии в обычной сборке: пока demo активен, токены и настройки живут в памяти, а реальные
// остаются нетронутыми — demo их не читает, не переписывает и не переживает перезапуск процесса.
internal class DemoStorage(realSecureStore: SecureStore, realSettings: Settings) {
    private var secrets: MutableMap<String, String>? = null
    private var values: MutableMap<String, String>? = null

    val secureStore: SecureStore = DemoSecureStore(realSecureStore)
    val settings: Settings = DemoSettings(realSettings)

    fun enter() {
        secrets = mutableMapOf()
        values = mutableMapOf()
    }

    fun leave() {
        secrets = null
        values = null
    }

    private inner class DemoSecureStore(private val real: SecureStore) : SecureStore {
        override suspend fun read(key: String): String? {
            val demo = secrets ?: return real.read(key)
            return demo[key]
        }

        override suspend fun write(key: String, value: String) {
            val demo = secrets ?: return real.write(key, value)
            demo[key] = value
        }

        override suspend fun remove(key: String) {
            val demo = secrets ?: return real.remove(key)
            demo.remove(key)
        }
    }

    private inner class DemoSettings(private val real: Settings) : Settings {
        override suspend fun getString(key: String): String? {
            val demo = values ?: return real.getString(key)
            return demo[key]
        }

        override suspend fun putString(key: String, value: String) {
            val demo = values ?: return real.putString(key, value)
            demo[key] = value
        }

        override suspend fun remove(key: String) {
            val demo = values ?: return real.remove(key)
            demo.remove(key)
        }
    }
}
