package app.rybbitclient.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpRedirectTest {

    @Test
    fun `authenticated requests never follow a cross-host redirect with the session cookie`() = runBlocking {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond(
                content = "",
                status = HttpStatusCode.Found,
                headers = headersOf(HttpHeaders.Location, "https://auth.example.com/login"),
            )
        }
        val api = RybbitApi(
            HttpClientFactory("RybbitAndroid/test", engine).create(Credential.Session("better-auth.session_token", "secret")),
            "https://analytics.example.com",
        )

        val failure = runCatching { api.organizations() }.exceptionOrNull()

        assertTrue(failure is ApiException)
        assertEquals(1, requests.size)
        assertEquals("analytics.example.com", requests.single().url.host)
        assertEquals("better-auth.session_token=secret", requests.single().headers[HttpHeaders.Cookie])
    }

    @Test
    fun `api key requests do not follow redirects either`() = runBlocking {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond(content = "", status = HttpStatusCode.MovedPermanently, headers = headersOf(HttpHeaders.Location, "https://other.example.com/"))
        }
        val api = RybbitApi(HttpClientFactory("RybbitAndroid/test", engine).create(Credential.ApiKey("k")), "https://analytics.example.com")

        runCatching { api.organizations() }

        assertEquals(1, requests.size)
        assertNull(requests.single().headers[HttpHeaders.Cookie])
    }
}
