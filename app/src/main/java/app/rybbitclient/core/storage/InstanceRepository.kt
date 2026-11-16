package app.rybbitclient.core.storage

import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.RybbitJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer

/**
 * Persists the instance list, the active instance and the last selected site per instance.
 * Instance metadata goes to [store] as JSON; credentials go only to [secrets].
 * Writes are serialized by a mutex because each one reads the list, changes it and writes it back.
 */
class InstanceRepository(
    private val store: KeyValueStore,
    private val secrets: SecretStore,
) {

    private val mutex = Mutex()
    private val instanceListSerializer = ListSerializer(Instance.serializer())

    val instances: Flow<List<Instance>> =
        store.observe(KEY_INSTANCES).map { decodeInstances(it) }.distinctUntilChanged()

    val activeInstanceId: Flow<String?> = store.observe(KEY_ACTIVE_INSTANCE).distinctUntilChanged()

    val activeInstance: Flow<Instance?> = combine(instances, activeInstanceId) { list, activeId ->
        list.firstOrNull { it.id == activeId }
    }

    /** The site id last opened on [instanceId], or null. Stored per instance so switching back restores it. */
    fun lastSelectedSiteId(instanceId: String): Flow<String?> = store.observe(siteKey(instanceId)).distinctUntilChanged()

    suspend fun instance(instanceId: String): Instance? = current().firstOrNull { it.id == instanceId }

    /**
     * Adds an instance with its credential. The first instance added becomes active automatically.
     * @throws IllegalArgumentException when an instance with the same id already exists.
     */
    suspend fun add(instance: Instance, credential: Credential) = mutex.withLock {
        val list = current()
        require(list.none { it.id == instance.id }) { "Instance ${instance.id} already exists" }
        secrets.save(instance.id, credential)
        writeList(list + instance)
        if (store.read(KEY_ACTIVE_INSTANCE) == null) store.write(KEY_ACTIVE_INSTANCE, instance.id)
    }

    /**
     * Updates the metadata of an existing instance (name, URL, account label). The credential is unchanged.
     * @throws IllegalArgumentException when the instance does not exist.
     */
    suspend fun update(instance: Instance) = mutex.withLock {
        val list = current()
        require(list.any { it.id == instance.id }) { "Instance ${instance.id} does not exist" }
        writeList(list.map { if (it.id == instance.id) instance else it })
    }

    /** Replaces the stored credential, e.g. after a re-login. */
    suspend fun updateCredential(instanceId: String, credential: Credential) = mutex.withLock {
        require(current().any { it.id == instanceId }) { "Instance $instanceId does not exist" }
        secrets.save(instanceId, credential)
    }

    /** Credential for [instanceId], or null when missing or unreadable. */
    suspend fun credentialFor(instanceId: String): Credential? = secrets.load(instanceId)

    /**
     * Makes [instanceId] the active instance.
     * @throws IllegalArgumentException when the instance does not exist.
     */
    suspend fun setActive(instanceId: String) = mutex.withLock {
        require(current().any { it.id == instanceId }) { "Instance $instanceId does not exist" }
        store.write(KEY_ACTIVE_INSTANCE, instanceId)
    }

    /**
     * Removes an instance and its credential. If it was active, the first remaining instance becomes active
     * (or nothing, when none remain). Removing an unknown id is a no-op.
     */
    suspend fun remove(instanceId: String) = mutex.withLock {
        val list = current()
        if (list.none { it.id == instanceId }) return@withLock
        secrets.delete(instanceId)
        val remaining = list.filterNot { it.id == instanceId }
        writeList(remaining)
        store.write(siteKey(instanceId), null)
        if (store.read(KEY_ACTIVE_INSTANCE) == instanceId) {
            store.write(KEY_ACTIVE_INSTANCE, remaining.firstOrNull()?.id)
        }
    }

    /** Remembers the site last opened on [instanceId]. Pass null to forget it. */
    suspend fun setLastSelectedSiteId(instanceId: String, siteId: String?) {
        store.write(siteKey(instanceId), siteId)
    }

    private suspend fun current(): List<Instance> = decodeInstances(store.read(KEY_INSTANCES))

    private suspend fun writeList(list: List<Instance>) {
        store.write(KEY_INSTANCES, RybbitJson.json.encodeToString(instanceListSerializer, list))
    }

    private fun decodeInstances(raw: String?): List<Instance> =
        if (raw.isNullOrBlank()) emptyList() else RybbitJson.json.decodeFromString(instanceListSerializer, raw)

    private fun siteKey(instanceId: String): String = "last_site.$instanceId"

    private companion object {
        const val KEY_INSTANCES = "instances"
        const val KEY_ACTIVE_INSTANCE = "active_instance_id"
    }
}
