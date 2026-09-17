package tech.stit.mobile.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Ключ не покидает Keystore, в файле только шифротекст; один экземпляр на процесс — DataStore запрещает два на один файл.
class KeystoreSecureStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    keySource: () -> SecretKey,
) : SecureStore {
    constructor(context: Context) : this(PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(FILE_NAME) }, ::keystoreKey)

    private val secretKey: SecretKey by lazy(keySource)

    override suspend fun read(key: String): String? = dataStore.data.first()[stringPreferencesKey(key)]?.let(::decrypt)

    override suspend fun write(key: String, value: String) {
        val encrypted = encrypt(value)
        dataStore.edit { it[stringPreferencesKey(key)] = encrypted }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey) }
        return Base64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(plain.encodeToByteArray()))
    }

    // Ключ Keystore не бэкапится: после восстановления на другом устройстве шифротекст нечитаем и считается отсутствующим.
    private fun decrypt(encoded: String): String? = runCatching {
        val bytes = Base64.getDecoder().decode(encoded)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        }
        cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).decodeToString()
    }.getOrNull()

    private companion object {
        const val FILE_NAME = "secure_store"
        const val KEY_ALIAS = "tech.stit.mobile.secure_store"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128

        fun keystoreKey(): SecretKey {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
            val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(spec) }.generateKey()
        }
    }
}
