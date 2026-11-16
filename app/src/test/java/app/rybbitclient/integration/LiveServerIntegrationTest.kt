package app.rybbitclient.integration

import app.rybbitclient.core.auth.AuthService
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.HttpClientFactory
import app.rybbitclient.core.network.RybbitApi
import app.rybbitclient.core.network.Unauthorized
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * End-to-end checks against a real Rybbit server over the real OkHttp engine. Skipped unless
 * `RYBBIT_IT_URL`, `RYBBIT_IT_EMAIL`, `RYBBIT_IT_PASSWORD` and `RYBBIT_IT_SITE` are set, e.g. against
 * the docker-compose self-hosted stack on `http://localhost:3001`.
 *
 * Covers what fixture tests can't: the Secure session cookie captured and replayed manually over
 * plain http, API-key minting, and decoding live responses.
 */
class LiveServerIntegrationTest {

    private val enteredUrl = System.getenv("RYBBIT_IT_URL").orEmpty()

    /** Like the app: resolve the entered address through the unauthenticated probe, then use only that. */
    private val baseUrl: String by lazy { runBlocking { auth.probe(enteredUrl) } }
    private val email = System.getenv("RYBBIT_IT_EMAIL").orEmpty()
    private val password = System.getenv("RYBBIT_IT_PASSWORD").orEmpty()
    private val siteId = System.getenv("RYBBIT_IT_SITE").orEmpty()
    private val factory = HttpClientFactory(userAgent = HttpClientFactory.userAgent("it"))
    private val auth = AuthService(factory, deviceModel = "JVM integration test")
    private val query = AnalyticsQuery(
        range = DateRange.Custom(LocalDate.now().minusDays(30), LocalDate.now()),
    )

    @Before
    fun requireServer() {
        assumeTrue("live server not configured", listOf(enteredUrl, email, password, siteId).all { it.isNotBlank() })
    }

    @Test
    fun passwordSignInMintsWorkingApiKeyOverHttp() = runBlocking<Unit> {
        val credential = auth.signInWithPassword(baseUrl, email, password, "JVM integration test")
        assertTrue("expected an API key, got $credential", credential is Credential.ApiKey)

        val orgs = auth.signInWithApiKey(baseUrl, (credential as Credential.ApiKey).key)
        assertTrue(orgs.isNotEmpty())
    }

    @Test
    fun wrongApiKeyIsUnauthorized() = runBlocking<Unit> {
        val result = runCatching { auth.signInWithApiKey(baseUrl, "definitely-not-a-key") }
        assertTrue(result.exceptionOrNull() is Unauthorized)
    }

    @Test
    fun dashboardEndpointsDecodeLiveResponses() = runBlocking<Unit> {
        val key = auth.signInWithPassword(baseUrl, email, password, "JVM integration test") as Credential.ApiKey
        factory.create(key).use { client ->
            val api = RybbitApi(client, baseUrl)
            val overview = api.overview(siteId, query)
            assertTrue((overview.pageviews ?: 0) > 0)
            assertTrue(api.overviewTimeSeries(siteId, query, query.defaultBucket()).isNotEmpty())
            assertTrue(api.metric(siteId, query, "pathname").data.isNotEmpty())
            assertTrue(api.sessions(siteId, query).isNotEmpty())
            val session = api.sessions(siteId, query).first()
            assertEquals(session.sessionId, api.sessionDetail(siteId, session.sessionId).session?.sessionId)
            assertTrue(api.events(siteId, query).data.isNotEmpty())
            api.eventNames(siteId, query)
            api.errorNames(siteId, query)
            api.goals(siteId, query)
            api.funnels(siteId)
            api.users(siteId, query)
            api.retention(siteId)
            api.liveUserCount(siteId)
        }
    }

    @Test
    fun cloudOnlyPagesRouteIsReportedAsUnavailable() = runBlocking<Unit> {
        val key = auth.signInWithPassword(baseUrl, email, password, "JVM integration test") as Credential.ApiKey
        factory.create(key).use { client ->
            val result = runCatching { RybbitApi(client, baseUrl).pages(siteId, query) }
            assertTrue("got ${result.exceptionOrNull()}", result.isSuccess || result.exceptionOrNull() is FeatureUnavailable)
        }
    }
}
