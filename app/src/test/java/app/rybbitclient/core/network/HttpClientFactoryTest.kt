package app.rybbitclient.core.network

import app.rybbitclient.testsupport.BASE_URL
import app.rybbitclient.testsupport.USER_AGENT
import app.rybbitclient.testsupport.respondJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class HttpClientFactoryTest {

    private fun captureRequest(credential: Credential?): HttpRequestData = runBlocking {
        var seen: HttpRequestData? = null
        val engine = MockEngine { request: HttpRequestData ->
            seen = request
            respondJson("[]", HttpStatusCode.OK)
        }
        val client = HttpClientFactory(USER_AGENT, engine).create(credential)
        client.get("$BASE_URL/api/organizations")
        seen!!
    }

    @Test
    fun `api key becomes a bearer header`() {
        val request = captureRequest(Credential.ApiKey("secret-key"))
        assertEquals("Bearer secret-key", request.headers[HttpHeaders.Authorization])
        assertNull(request.headers[HttpHeaders.Cookie])
    }

    @Test
    fun `session becomes a manual cookie header with the stored name`() {
        val request = captureRequest(Credential.Session("__Secure-better-auth.session_token", "abc.def"))
        assertEquals("__Secure-better-auth.session_token=abc.def", request.headers[HttpHeaders.Cookie])
        assertNull(request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `user agent carries the app version`() {
        val request = captureRequest(null)
        assertEquals("RybbitAndroid/0.1.0-test", request.headers[HttpHeaders.UserAgent])
        assertEquals("RybbitAndroid/0.1.0", HttpClientFactory.userAgent("0.1.0"))
    }

    @Test
    fun `no credential sends no auth headers`() {
        val request = captureRequest(null)
        assertNull(request.headers[HttpHeaders.Authorization])
        assertFalse(request.headers.contains(HttpHeaders.Cookie))
    }

    @Test
    fun `credentials never appear in toString`() {
        assertFalse(Credential.ApiKey("very-secret").toString().contains("very-secret"))
        assertFalse(Credential.Session("name", "very-secret").toString().contains("very-secret"))
    }
}
