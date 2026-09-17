package tech.stit.mobile.core.lib

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
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataStoreSettingsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val settings by lazy {
        DataStoreSettings(PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("settings.preferences_pb") })
    }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun storesStringsPerKey() = runBlocking {
        assertNull(settings.getString(SettingsKeys.THEME))
        settings.putString(SettingsKeys.THEME, "Dark")
        settings.putString(SettingsKeys.LANGUAGE, "Ru")
        assertEquals("Dark", settings.getString(SettingsKeys.THEME))
        assertEquals("Ru", settings.getString(SettingsKeys.LANGUAGE))
        settings.remove(SettingsKeys.THEME)
        assertNull(settings.getString(SettingsKeys.THEME))
        assertEquals("Ru", settings.getString(SettingsKeys.LANGUAGE))
    }
}
