package app.rybbitclient.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json

/**
 * Builds a Ktor client for one credential. [engine] is injectable so tests can use a fake engine;
 * production uses OkHttp. Credentials are attached as default headers, never through a cookie jar.
 */
class HttpClientFactory(
    private val userAgent: String,
    private val engine: HttpClientEngine? = null,
) {

    /**
     * @param credential attached as `Authorization` or `Cookie`; null for unauthenticated calls.
     * @param origin sent as the `Origin` header when set. Required by Rybbit's email sign-in and
     * session-auth endpoints (missing Origin gets 403 `MISSING_OR_NULL_ORIGIN`).
     */
    fun create(credential: Credential? = null, origin: String? = null): HttpClient {
        val config: HttpClientConfig<*>.() -> Unit = { configure(credential, origin) }
        return if (engine != null) HttpClient(engine, config) else HttpClient(OkHttp, config)
    }

    private fun HttpClientConfig<*>.configure(credential: Credential?, origin: String?) {
        expectSuccess = false
        // A redirect could hand the session cookie or bearer key to another host (an SSO or forward-auth
        // proxy, for example). Authenticated clients therefore surface 3xx as an error instead of following it.
        followRedirects = credential == null
        install(ContentNegotiation) { json(RybbitJson.json) }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
        install(DefaultRequest) {
            header(HttpHeaders.UserAgent, userAgent)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            if (origin != null) header(HttpHeaders.Origin, origin)
            when (credential) {
                is Credential.ApiKey -> header(HttpHeaders.Authorization, "Bearer ${credential.key}")
                is Credential.Session -> header(HttpHeaders.Cookie, "${credential.cookieName}=${credential.cookieValue}")
                null -> Unit
            }
        }
    }

    companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000L
        const val SOCKET_TIMEOUT_MILLIS = 30_000L
        const val REQUEST_TIMEOUT_MILLIS = 60_000L
        const val USER_AGENT_PREFIX = "RybbitAndroid"

        /** Builds the User-Agent string, e.g. `RybbitAndroid/0.1.0`. */
        fun userAgent(versionName: String): String = "$USER_AGENT_PREFIX/$versionName"
    }
}
