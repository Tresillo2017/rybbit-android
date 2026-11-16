package app.rybbitclient.core.auth

import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.HttpClientFactory
import app.rybbitclient.core.network.InvalidCredentials
import app.rybbitclient.core.network.InvalidResponse
import app.rybbitclient.core.network.Network
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.network.Unauthorized
import app.rybbitclient.testsupport.BASE_URL
import app.rybbitclient.testsupport.FixtureFiles
import app.rybbitclient.testsupport.respondJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import app.rybbitclient.testsupport.cookieHeaders
import io.ktor.client.engine.mock.respond
import io.ktor.http.content.TextContent
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AuthServiceTest {

    private val productionCookie = "__Secure-better-auth.session_token=sess123.sig; Path=/; HttpOnly; Secure; SameSite=Lax"
    private val devCookie = "better-auth.session_token=dev456; Path=/; HttpOnly"

    /** Routes requests by path. Each handler returns a response; all requests are recorded. */
    private class Routes(private val handlers: Map<String, MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>) {
        val seen = mutableListOf<HttpRequestData>()

        val engine = MockEngine { request ->
            seen += request
            val handler = handlers[request.url.encodedPath]
                ?: error("Unexpected request ${request.method.value} ${request.url}")
            handler(request)
        }

        fun service(): AuthService = AuthService(HttpClientFactory("RybbitAndroid/test", engine), deviceModel = "Pixel Test")

        fun request(path: String): HttpRequestData = seen.first { it.url.encodedPath == path }
    }

    private fun MockRequestHandleScope.sessionResponse(vararg cookies: String): HttpResponseData {
        val headers = cookieHeaders(*cookies)
        return respond(
            content = """{"token":"not-a-bearer","user":{"id":"u1"}}""",
            status = HttpStatusCode.OK,
            headers = headers,
        )
    }

    private fun health() = mapOf<String, MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>(
        "/api/health" to { respondJson("""{"status":"ok"}""", HttpStatusCode.OK) },
    )

    @Test
    fun `probe succeeds when health returns 200`() = runBlocking {
        val routes = Routes(health())
        routes.service().probe(BASE_URL)
        assertEquals(1, routes.seen.size)
    }

    @Test
    fun `probe maps a missing server to Server error`() = runBlocking {
        val routes = Routes(mapOf("/api/health" to { respondJson("<html/>", HttpStatusCode.BadGateway) }))
        assertTrue(expectFailure { routes.service().probe(BASE_URL) } is Server)
    }

    @Test
    fun `probe maps transport failure to Network`() = runBlocking {
        val engine = MockEngine { throw java.io.IOException("no route") }
        val service = AuthService(HttpClientFactory("RybbitAndroid/test", engine), deviceModel = "Test")
        assertTrue(expectFailure { service.probe(BASE_URL) } is Network)
    }

    @Test
    fun `api key sign in returns organizations and sends bearer`() = runBlocking {
        val routes = Routes(
            mapOf("/api/organizations" to { respondJson(FixtureFiles.read("organizations.json"), HttpStatusCode.OK) }),
        )
        val orgs = routes.service().signInWithApiKey(BASE_URL, "rk_live_123")
        assertEquals("Demo Org", orgs.single().name)
        assertEquals("Bearer rk_live_123", routes.request("/api/organizations").headers[HttpHeaders.Authorization])
    }

    @Test
    fun `rejected api key surfaces Unauthorized`() = runBlocking {
        val routes = Routes(
            mapOf("/api/organizations" to { respondJson(FixtureFiles.read("unauthorized.json"), HttpStatusCode.Unauthorized) }),
        )
        assertTrue(expectFailure { routes.service().signInWithApiKey(BASE_URL, "bad") } is Unauthorized)
    }

    @Test
    fun `password sign in exchanges the session for an api key`() = runBlocking {
        val routes = Routes(
            mapOf(
                "/api/auth/sign-in/email" to { sessionResponse(productionCookie) },
                "/api/user/api-keys" to { respondJson("""{"key":"rybbit_key_42"}""", HttpStatusCode.OK) },
            ),
        )
        val credential = routes.service().signInWithPassword(BASE_URL, "me@example.com", "pw", "Pixel 8")

        assertEquals(Credential.ApiKey("rybbit_key_42"), credential)
        val signIn = routes.request("/api/auth/sign-in/email")
        assertEquals(HttpMethod.Post, signIn.method)
        assertEquals(BASE_URL, signIn.headers[HttpHeaders.Origin])
        assertTrue((signIn.body as TextContent).text.contains("\"email\":\"me@example.com\""))
        val keyRequest = routes.request("/api/user/api-keys")
        assertEquals("__Secure-better-auth.session_token=sess123.sig", keyRequest.headers[HttpHeaders.Cookie])
        assertEquals(BASE_URL, keyRequest.headers[HttpHeaders.Origin])
        assertTrue((keyRequest.body as TextContent).text.contains("Rybbit Android (Pixel 8)"))
    }

    @Test
    fun `dev cookie name is captured from the header, not hardcoded`() = runBlocking {
        val routes = Routes(
            mapOf(
                "/api/auth/sign-in/email" to { sessionResponse("other=1", devCookie) },
                "/api/user/api-keys" to { respondError(HttpStatusCode.InternalServerError) },
            ),
        )
        val credential = routes.service().signInWithPassword(BASE_URL, "a@b.c", "pw", "")
        assertEquals(Credential.Session("better-auth.session_token", "dev456"), credential)
        assertEquals(
            "better-auth.session_token=dev456",
            routes.request("/api/user/api-keys").headers[HttpHeaders.Cookie],
        )
    }

    @Test
    fun `key creation failure falls back to the session credential`() = runBlocking {
        val routes = Routes(
            mapOf(
                "/api/auth/sign-in/email" to { sessionResponse(productionCookie) },
                "/api/user/api-keys" to { respondJson("""{"error":"Old server","code":"NOT_IMPLEMENTED"}""", HttpStatusCode.NotImplemented) },
            ),
        )
        val credential = routes.service().signInWithPassword(BASE_URL, "a@b.c", "pw", "Phone")
        assertEquals(Credential.Session("__Secure-better-auth.session_token", "sess123.sig"), credential)
    }

    @Test
    fun `key creation forbidden also falls back to the session`() = runBlocking {
        val routes = Routes(
            mapOf(
                "/api/auth/sign-in/email" to { sessionResponse(productionCookie) },
                "/api/user/api-keys" to { respondJson("""{"error":"Forbidden","code":"FORBIDDEN"}""", HttpStatusCode.Forbidden) },
            ),
        )
        assertTrue(routes.service().signInWithPassword(BASE_URL, "a@b.c", "pw", "Phone") is Credential.Session)
    }

    @Test
    fun `key creation 401 is rethrown as Unauthorized`() = runBlocking {
        val routes = Routes(
            mapOf(
                "/api/auth/sign-in/email" to { sessionResponse(productionCookie) },
                "/api/user/api-keys" to { respondJson("""{"error":"Unauthorized","code":"AUTHENTICATION_REQUIRED"}""", HttpStatusCode.Unauthorized) },
            ),
        )
        assertTrue(expectFailure { routes.service().signInWithPassword(BASE_URL, "a@b.c", "pw", "Phone") } is Unauthorized)
    }

    @Test
    fun `key creation network failure falls back to the session`() = runBlocking {
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            if (request.url.encodedPath == "/api/auth/sign-in/email") {
                respond(content = "{}", status = HttpStatusCode.OK, headers = cookieHeaders(productionCookie))
            } else {
                throw java.io.IOException("dropped")
            }
        }
        val service = AuthService(HttpClientFactory("RybbitAndroid/test", engine), deviceModel = "Test")
        assertTrue(service.signInWithPassword(BASE_URL, "a@b.c", "pw", "Phone") is Credential.Session)
        assertEquals(2, calls)
    }

    @Test
    fun `wrong password with 401 maps to InvalidCredentials`() = runBlocking {
        val routes = Routes(
            mapOf("/api/auth/sign-in/email" to { respondJson("""{"code":"INVALID_EMAIL_OR_PASSWORD","message":"Invalid email or password"}""", HttpStatusCode.Unauthorized) }),
        )
        val failure = expectFailure { routes.service().signInWithPassword(BASE_URL, "a@b.c", "bad", "Phone") }
        assertTrue(failure is InvalidCredentials)
        assertEquals("INVALID_EMAIL_OR_PASSWORD", (failure as InvalidCredentials).code)
    }

    @Test
    fun `wrong password with 400 code maps to InvalidCredentials`() = runBlocking {
        val routes = Routes(
            mapOf("/api/auth/sign-in/email" to { respondJson("""{"code":"INVALID_EMAIL_OR_PASSWORD"}""", HttpStatusCode.BadRequest) }),
        )
        assertTrue(expectFailure { routes.service().signInWithPassword(BASE_URL, "a@b.c", "bad", "Phone") } is InvalidCredentials)
    }

    @Test
    fun `other 400 on sign in is a plain client error, not a password error`() = runBlocking {
        val routes = Routes(
            mapOf("/api/auth/sign-in/email" to { respondJson("""{"code":"BAD_EMAIL"}""", HttpStatusCode.BadRequest) }),
        )
        val failure = expectFailure { routes.service().signInWithPassword(BASE_URL, "nope", "pw", "Phone") }
        assertTrue(failure !is InvalidCredentials)
    }

    @Test
    fun `missing session cookie is an InvalidResponse`() = runBlocking {
        val routes = Routes(mapOf("/api/auth/sign-in/email" to { sessionResponse() }))
        assertTrue(expectFailure { routes.service().signInWithPassword(BASE_URL, "a@b.c", "pw", "Phone") } is InvalidResponse)
    }

    @Test
    fun `blank device name uses the device model in the key name`() = runBlocking {
        val routes = Routes(
            mapOf(
                "/api/auth/sign-in/email" to { sessionResponse(productionCookie) },
                "/api/user/api-keys" to { respondJson("""{"key":"k"}""", HttpStatusCode.OK) },
            ),
        )
        routes.service().signInWithPassword(BASE_URL, "a@b.c", "pw", "  ")
        assertTrue((routes.request("/api/user/api-keys").body as TextContent).text.contains("Rybbit Android (Pixel Test)"))
    }

    @Test
    fun `session cookie parser picks session token and ignores others`() {
        val picked = SessionCookie.fromSetCookieHeaders(listOf("theme=dark; Path=/", productionCookie))
        assertEquals(Credential.Session("__Secure-better-auth.session_token", "sess123.sig"), picked)
        assertEquals(null, SessionCookie.fromSetCookieHeaders(listOf("theme=dark")))
        assertEquals(null, SessionCookie.fromSetCookieHeaders(listOf("better-auth.session_token=; Path=/")))
    }

    private suspend fun expectFailure(block: suspend () -> Unit): ApiException {
        try {
            block()
        } catch (failure: ApiException) {
            return failure
        }
        fail("Expected ApiException")
        throw IllegalStateException("unreachable")
    }
}
