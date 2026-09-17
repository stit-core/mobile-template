package tech.stit.mobile.core.auth

// Keystore AES-GCM + DataStore на Android, Keychain на iOS; access token сюда не пишется — только память (07 §1.3b).
interface SecureStore {
    suspend fun read(key: String): String?

    suspend fun write(key: String, value: String)

    suspend fun remove(key: String)
}
