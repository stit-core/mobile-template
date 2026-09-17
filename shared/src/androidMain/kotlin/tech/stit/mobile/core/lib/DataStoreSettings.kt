package tech.stit.mobile.core.lib

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first

// Настройки в DataStore Preferences; один экземпляр на процесс — DataStore запрещает два на один файл.
class DataStoreSettings internal constructor(private val dataStore: DataStore<Preferences>) : Settings {
    constructor(context: Context) : this(PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(FILE_NAME) })

    override suspend fun getString(key: String): String? = dataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun putString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }

    private companion object {
        const val FILE_NAME = "settings"
    }
}
