package app.rybbitclient.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** [KeyValueStore] backed by a Preferences DataStore. Android glue, exercised on device. */
class DataStoreKeyValueStore(
    private val dataStore: DataStore<Preferences>,
) : KeyValueStore {

    override fun observe(key: String): Flow<String?> {
        val preferenceKey = stringPreferencesKey(key)
        return dataStore.data.map { it[preferenceKey] }.distinctUntilChanged()
    }

    override suspend fun write(key: String, value: String?) {
        val preferenceKey = stringPreferencesKey(key)
        dataStore.edit { prefs ->
            if (value == null) prefs.remove(preferenceKey) else prefs[preferenceKey] = value
        }
    }
}
