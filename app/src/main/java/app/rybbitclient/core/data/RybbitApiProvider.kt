package app.rybbitclient.core.data

import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.HttpClientFactory
import app.rybbitclient.core.network.RybbitApi
import app.rybbitclient.core.network.Unauthorized
import app.rybbitclient.core.storage.InstanceRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Hands out one cached [RybbitApi] (and its HttpClient) per instance. An entry is rebuilt when the
 * stored credential or base URL changes, and closed when the instance is evicted.
 */
class RybbitApiProvider(
    private val instances: InstanceRepository,
    private val httpFactory: HttpClientFactory,
) {

    private val mutex = Mutex()
    private val cache = mutableMapOf<String, CachedApi>()

    /**
     * @throws IllegalStateException when the instance does not exist.
     * @throws Unauthorized when the instance has no readable credential, so the UI asks to sign in again.
     */
    suspend fun forInstance(instanceId: String): RybbitApi = mutex.withLock {
        val instance = instances.instance(instanceId)
            ?: throw IllegalStateException("Instance $instanceId does not exist")
        val credential = instances.credentialFor(instanceId)
            ?: throw Unauthorized(code = null, serverMessage = null, resolution = "Sign in again to this instance.")
        val cached = cache[instanceId]
        if (cached != null && cached.credential == credential && cached.api.baseUrl == instance.baseUrl) {
            return@withLock cached.api
        }
        cached?.client?.close()
        val client = httpFactory.create(credential)
        val api = RybbitApi(client, instance.baseUrl)
        cache[instanceId] = CachedApi(credential, client, api)
        api
    }

    /** Closes and forgets the client for [instanceId]. Call when the instance is removed. */
    suspend fun evict(instanceId: String) = mutex.withLock {
        cache.remove(instanceId)?.client?.close()
    }

    private class CachedApi(
        val credential: Credential,
        val client: HttpClient,
        val api: RybbitApi,
    )
}
