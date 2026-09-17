package tech.stit.mobile.core.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

// Keystore недоступен вне устройства: ключ подставляется, проверяется AES-GCM поверх DataStore.
class KeystoreSecureStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val file by lazy { folder.newFile("secure.preferences_pb") }
    private val dataStore by lazy { PreferenceDataStoreFactory.create(scope = scope) { file } }
    private val store by lazy { KeystoreSecureStore(dataStore) { freshKey() } }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun roundTripsAndRemoves() = runBlocking {
        assertNull(store.read("auth.refresh_token"))
        store.write("auth.refresh_token", "refresh-1")
        assertEquals("refresh-1", store.read("auth.refresh_token"))
        store.write("auth.refresh_token", "refresh-2")
        assertEquals("refresh-2", store.read("auth.refresh_token"))
        store.remove("auth.refresh_token")
        assertNull(store.read("auth.refresh_token"))
    }

    @Test
    fun fileHoldsOnlyCiphertext() = runBlocking {
        store.write("auth.refresh_token", "very-secret-token")
        assertFalse(file.readText(Charsets.ISO_8859_1).contains("very-secret-token"))
    }

    @Test
    fun ciphertextUnderAnotherKeyReadsAsMissing() = runBlocking {
        store.write("k", "v")
        assertEquals("v", store.read("k"))
        assertNull(KeystoreSecureStore(dataStore) { freshKey() }.read("k"))
    }

    private fun freshKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(KEY_BITS) }.generateKey()

    private companion object {
        const val KEY_BITS = 256
    }
}
