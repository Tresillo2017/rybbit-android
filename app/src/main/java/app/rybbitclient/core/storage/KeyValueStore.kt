package app.rybbitclient.core.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Minimal string key/value persistence. The production implementation wraps Preferences DataStore;
 * tests use [InMemoryKeyValueStore]. Null values remove the key.
 */
interface KeyValueStore {
    fun observe(key: String): Flow<String?>

    suspend fun read(key: String): String? = observe(key).first()

    suspend fun write(key: String, value: String?)
}

/** Volatile store for unit tests and previews. */
class InMemoryKeyValueStore : KeyValueStore {
    private val state = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observe(key: String): Flow<String?> = state.map { it[key] }.distinctUntilChanged()

    override suspend fun write(key: String, value: String?) {
        state.update { current -> if (value == null) current - key else current + (key to value) }
    }
}
