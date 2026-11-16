package app.rybbitclient.core.storage

import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.RybbitJson
import kotlinx.serialization.SerializationException
import java.security.GeneralSecurityException

/** The only place credentials are read or written. Keyed by instance id. */
interface SecretStore {
    suspend fun save(instanceId: String, credential: Credential)

    /** Returns null when nothing is stored or the stored secret can no longer be decrypted (re-login needed). */
    suspend fun load(instanceId: String): Credential?

    suspend fun delete(instanceId: String)
}

/** Volatile secret store for unit tests. */
class InMemorySecretStore : SecretStore {
    private val credentials = mutableMapOf<String, Credential>()

    override suspend fun save(instanceId: String, credential: Credential) {
        credentials[instanceId] = credential
    }

    override suspend fun load(instanceId: String): Credential? = credentials[instanceId]

    override suspend fun delete(instanceId: String) {
        credentials.remove(instanceId)
    }
}

/**
 * Serializes each credential to JSON, encrypts it with [cipher] and stores the ciphertext in [store].
 * The plaintext secret never reaches the DataStore.
 */
class KeystoreSecretStore(
    private val cipher: SecretCipher,
    private val store: KeyValueStore,
) : SecretStore {

    override suspend fun save(instanceId: String, credential: Credential) {
        val json = RybbitJson.json.encodeToString(Credential.serializer(), credential)
        store.write(keyFor(instanceId), cipher.encrypt(json))
    }

    override suspend fun load(instanceId: String): Credential? {
        val encoded = store.read(keyFor(instanceId)) ?: return null
        return try {
            RybbitJson.json.decodeFromString(Credential.serializer(), cipher.decrypt(encoded))
        } catch (unreadable: GeneralSecurityException) {
            null
        } catch (unreadable: IllegalArgumentException) {
            null
        } catch (unreadable: SerializationException) {
            null
        }
    }

    override suspend fun delete(instanceId: String) {
        store.write(keyFor(instanceId), null)
    }

    private fun keyFor(instanceId: String): String = "secret.$instanceId"
}
