package app.rybbitclient.core.data

import app.rybbitclient.core.model.AuthType
import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.model.InstanceKind
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.HttpClientFactory
import app.rybbitclient.core.network.Unauthorized
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.storage.InMemoryKeyValueStore
import app.rybbitclient.core.storage.InMemorySecretStore
import app.rybbitclient.core.storage.InstanceRepository
import app.rybbitclient.testsupport.FixtureFiles
import app.rybbitclient.testsupport.respondJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AnalyticsRepositoryTest {

    private val preferences = InMemoryKeyValueStore()
    private val secrets = InMemorySecretStore()
    private val instances = InstanceRepository(preferences, secrets)
    private val requests = mutableListOf<HttpRequestData>()
    private val query = AnalyticsQuery(DateRange.AllTime)

    private val engine = MockEngine { request ->
        requests += request
        when (request.url.encodedPath) {
            "/api/sites/1/pages" -> respondJson(FixtureFiles.read("pages.json"), HttpStatusCode.NotFound)
            "/api/sites/1/overview" -> respondJson(FixtureFiles.read("overview.json"), HttpStatusCode.OK)
            "/api/sites/1/live-user-count" -> respondJson(FixtureFiles.read("live_user_count.json"), HttpStatusCode.OK)
            else -> respondJson("{}", HttpStatusCode.NotFound)
        }
    }
    private val httpFactory = HttpClientFactory("RybbitAndroid/test", engine)
    private val provider = RybbitApiProvider(instances, httpFactory)
    private val features = FeatureAvailability()
    private val repository = DefaultAnalyticsRepository(instances, provider, features)

    private fun instance(id: String, url: String) = Instance(
        id = id,
        displayName = id,
        baseUrl = url,
        kind = InstanceKind.SelfHosted,
        authType = AuthType.ApiKey,
        createdAtMillis = 0L,
    )

    @Test
    fun `api provider reuses the client for the same instance`() = runBlocking {
        instances.add(instance("a", "https://a.test"), Credential.ApiKey("k"))
        val first = provider.forInstance("a")
        assertSame(first, provider.forInstance("a"))
    }

    @Test
    fun `api provider rebuilds after the credential changes`() = runBlocking {
        instances.add(instance("a", "https://a.test"), Credential.ApiKey("k"))
        val first = provider.forInstance("a")
        instances.updateCredential("a", Credential.ApiKey("k2"))
        assertNotSame(first, provider.forInstance("a"))
    }

    @Test
    fun `api provider rejects an unknown instance`() = runBlocking {
        instances.add(instance("a", "https://a.test"), Credential.ApiKey("k"))
        instances.remove("a")
        instances.add(instance("b", "https://b.test"), Credential.ApiKey("k"))
        try {
            provider.forInstance("missing")
            fail("Expected failure for unknown instance")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("missing"))
        }
    }

    @Test
    fun `eviction forces a fresh client`() = runBlocking {
        instances.add(instance("a", "https://a.test"), Credential.ApiKey("k"))
        val first = provider.forInstance("a")
        provider.evict("a")
        assertNotSame(first, provider.forInstance("a"))
    }

    @Test
    fun `reads resolve the active instance base url at call time`() = runBlocking {
        instances.add(instance("a", "https://a.test"), Credential.ApiKey("k"))
        instances.add(instance("b", "https://b.test"), Credential.ApiKey("k"))
        instances.setActive("b")

        repository.overview("1", query)

        assertEquals("b.test", requests.single().url.host)
        assertEquals("Bearer k", requests.single().headers["Authorization"])
    }

    @Test
    fun `reads fail with NoActiveInstance when nothing is selected`() = runBlocking {
        try {
            repository.overview("1", query)
            fail("Expected NoActiveInstanceException")
        } catch (expected: NoActiveInstanceException) {
            assertTrue(requests.isEmpty())
        }
    }

    @Test
    fun `live user count unwraps to an int`() = runBlocking {
        instances.add(instance("a", "https://a.test"), Credential.ApiKey("k"))
        assertEquals(16, repository.liveUserCount("1"))
    }

    @Test
    fun `missing feature is recorded and later calls skip the request`() = runBlocking {
        instances.add(instance("cloud", "https://self.test"), Credential.ApiKey("k"))
        assertTrue(repository.isFeatureAvailable(Feature.Pages))

        try {
            repository.pages("1", query)
            fail("Expected FeatureUnavailable")
        } catch (expected: FeatureUnavailable) {
            // expected
        }
        assertFalse(repository.isFeatureAvailable(Feature.Pages))
        val sentBefore = requests.size

        try {
            repository.pages("1", query)
            fail("Expected FeatureUnavailable without a request")
        } catch (expected: FeatureUnavailable) {
            assertEquals(sentBefore, requests.size)
        }
    }

    @Test
    fun `feature gap on one instance does not hide it on another`() = runBlocking {
        instances.add(instance("self", "https://a.test"), Credential.ApiKey("k"))
        instances.add(instance("other", "https://b.test"), Credential.ApiKey("k"))
        features.markUnavailable("self", Feature.Pages)

        instances.setActive("other")
        assertTrue(repository.isFeatureAvailable(Feature.Pages))
    }

    @Test
    fun `removed instance loses its cached client and feature record`() = runBlocking {
        instances.add(instance("a", "https://a.test"), Credential.ApiKey("k"))
        features.markUnavailable("a", Feature.Pages)
        provider.forInstance("a")

        instances.remove("a")
        provider.evict("a")
        features.clear("a")

        assertTrue(features.isAvailable("a", Feature.Pages))
    }

    @Test
    fun `instance without a readable credential surfaces as Unauthorized`() = runBlocking {
        instances.add(instance("a", "https://a.test"), Credential.ApiKey("k"))
        secrets.delete("a")
        try {
            provider.forInstance("a")
            fail("Expected Unauthorized")
        } catch (expected: Unauthorized) {
            assertEquals("Sign in again to this instance.", expected.resolution)
        }
    }
}
