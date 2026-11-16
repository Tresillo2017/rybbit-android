package app.rybbitclient.core.network

import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.testsupport.FixtureFiles
import app.rybbitclient.testsupport.apiReplying
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ApiErrorMappingTest {

    private val query = AnalyticsQuery(DateRange.AllTime)

    private suspend fun expectFailure(block: suspend () -> Unit): ApiException {
        try {
            block()
        } catch (failure: ApiException) {
            return failure
        }
        fail("Expected an ApiException")
        throw IllegalStateException("unreachable")
    }

    @Test
    fun `unauthorized fixture maps to Unauthorized with parsed body`() = runBlocking {
        val api = apiReplying(FixtureFiles.read("unauthorized.json"), HttpStatusCode.Unauthorized)
        val failure = expectFailure { api.organizations() }
        assertTrue(failure is Unauthorized)
        val http = failure as HttpStatusException
        assertEquals("AUTHENTICATION_REQUIRED", http.code)
        assertEquals("Unauthorized", http.serverMessage)
        assertTrue(http.resolution!!.contains("Bearer"))
    }

    @Test
    fun `pages fixture maps to FeatureUnavailable`() = runBlocking {
        val api = apiReplying(FixtureFiles.read("pages.json"), HttpStatusCode.NotFound)
        val failure = expectFailure { api.pages("1", query) }
        assertTrue(failure is FeatureUnavailable)
        assertEquals(404, (failure as HttpStatusException).status)
    }

    @Test
    fun `not found site fixture maps to Forbidden`() = runBlocking {
        val api = apiReplying(FixtureFiles.read("not_found_site.json"), HttpStatusCode.Forbidden)
        val failure = expectFailure { api.site("999") }
        assertTrue(failure is Forbidden)
        assertEquals("FORBIDDEN", (failure as HttpStatusException).code)
    }

    @Test
    fun `404 without route code maps to NotFound`() = runBlocking {
        val api = apiReplying("""{"error":"Not Found","code":"NOT_FOUND","message":"no site"}""", HttpStatusCode.NotFound)
        assertTrue(expectFailure { api.site("1") } is NotFound)
    }

    @Test
    fun `429 uses Retry-After header`() = runBlocking {
        val body = """{"error":"Rate limit exceeded","scope":"burst","limit":50,"retryAfter":1}"""
        val api = apiReplying(body, HttpStatusCode.TooManyRequests, retryAfter = "7")
        val failure = expectFailure { api.organizations() } as RateLimited
        assertEquals(7L, failure.retryAfterSeconds)
    }

    @Test
    fun `429 falls back to retryAfter in body`() = runBlocking {
        val body = """{"error":"Rate limit exceeded","scope":"daily","limit":5000,"retryAfter":34200}"""
        val api = apiReplying(body, HttpStatusCode.TooManyRequests)
        assertEquals(34200L, (expectFailure { api.organizations() } as RateLimited).retryAfterSeconds)
    }

    @Test
    fun `429 without any retry hint leaves retryAfter null`() = runBlocking {
        val api = apiReplying("", HttpStatusCode.TooManyRequests)
        assertNull((expectFailure { api.organizations() } as RateLimited).retryAfterSeconds)
    }

    @Test
    fun `500 maps to Server with status`() = runBlocking {
        val api = apiReplying("<html>Internal error</html>", HttpStatusCode.InternalServerError)
        val failure = expectFailure { api.organizations() }
        assertTrue(failure is Server)
        assertEquals(500, (failure as Server).status)
    }

    @Test
    fun `other 4xx maps to ClientError`() = runBlocking {
        val api = apiReplying("""{"error":"Bad","code":"INVALID_REQUEST"}""", HttpStatusCode.BadRequest)
        val failure = expectFailure { api.organizations() } as ClientError
        assertEquals(400, failure.status)
        assertEquals("INVALID_REQUEST", failure.code)
    }

    @Test
    fun `malformed JSON success body maps to InvalidResponse`() = runBlocking {
        val api = apiReplying("{not json at all", HttpStatusCode.OK)
        assertTrue(expectFailure { api.organizations() } is InvalidResponse)
    }

    @Test
    fun `wrong shape success body maps to InvalidResponse`() = runBlocking {
        val api = apiReplying("""{"unexpected":true}""", HttpStatusCode.OK)
        assertTrue(expectFailure { api.overview("1", query) } is InvalidResponse)
    }

    @Test
    fun `transport failure maps to Network`() = runBlocking {
        val engine = MockEngine { throw java.io.IOException("unreachable") }
        val api = RybbitApi(HttpClientFactory("RybbitAndroid/test", engine).create(), BASE_URL_FOR_TEST)
        assertTrue(expectFailure { api.organizations() } is Network)
    }

    @Test
    fun `cancellation is rethrown and never wrapped`() = runBlocking {
        val engine = MockEngine { throw kotlinx.coroutines.CancellationException("cancelled") }
        val api = RybbitApi(HttpClientFactory("RybbitAndroid/test", engine).create(), BASE_URL_FOR_TEST)
        var caught: Throwable? = null
        try {
            api.organizations()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            caught = cancellation
        }
        assertTrue(caught is kotlinx.coroutines.CancellationException)
    }

    @Test
    fun `error mapper keeps server code for unknown statuses`() {
        val mapped = ApiErrorMapper.map(503, """{"code":"DOWN","message":"maintenance"}""", null)
        assertTrue(mapped is Server)
        assertEquals("DOWN", (mapped as HttpStatusException).code)
    }

    @Test
    fun `error mapper handles non JSON bodies`() {
        val mapped = ApiErrorMapper.map(502, "<html>bad gateway</html>", null)
        assertTrue(mapped is Server)
        assertNull((mapped as HttpStatusException).code)
    }

    private companion object {
        const val BASE_URL_FOR_TEST = "https://analytics.example.test"
    }
}
