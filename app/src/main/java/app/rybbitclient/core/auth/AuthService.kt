package app.rybbitclient.core.auth

import app.rybbitclient.core.model.CreateApiKeyRequest
import app.rybbitclient.core.model.Organization
import app.rybbitclient.core.network.ApiErrorMapper
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.HttpClientFactory
import app.rybbitclient.core.network.InvalidCredentials
import app.rybbitclient.core.network.InvalidResponse
import app.rybbitclient.core.network.Network
import app.rybbitclient.core.network.RybbitApi
import app.rybbitclient.core.network.RybbitJson
import app.rybbitclient.core.network.Unauthorized
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/** Email sign-in request body for `POST /api/auth/sign-in/email`. */
@Serializable
private data class EmailSignInRequest(val email: String, val password: String)

/** Only the machine code is needed from an auth error body. */
@Serializable
private data class AuthErrorBody(val code: String? = null)

/** A fully read HTTP response, so callers never touch a closed client. */
private class RawResponse(val status: HttpStatusCode, val headers: Headers, val text: String)

/** Auth flows for adding an instance. Persists nothing; callers store the returned credential. */
class AuthService(
    private val httpFactory: HttpClientFactory,
    private val deviceModel: String,
) {

    /** `GET /api/health`. Succeeds when a Rybbit server answers at [baseUrl]; throws [ApiException] otherwise. */
    suspend fun probe(baseUrl: String) {
        httpFactory.create().use { RybbitApi(it, baseUrl).healthCheck() }
    }

    /**
     * Validates an API key by listing organizations. Returns the organizations on success, or throws
     * [Unauthorized] when the key is rejected.
     */
    suspend fun signInWithApiKey(baseUrl: String, key: String): List<Organization> =
        httpFactory.create(Credential.ApiKey(key)).use { RybbitApi(it, baseUrl).organizations() }

    /**
     * Email/password sign-in. Captures the session cookie from `Set-Cookie`, then exchanges it for an
     * API key. If key creation fails with anything other than 401, the session cookie is kept as the
     * credential instead. A 401 on key creation means the session is already invalid and is rethrown.
     *
     * @throws InvalidCredentials when the email or password is rejected.
     * @throws InvalidResponse when sign-in succeeds but no session cookie is returned.
     */
    suspend fun signInWithPassword(
        baseUrl: String,
        email: String,
        password: String,
        deviceName: String,
    ): Credential {
        val session = signInForSession(baseUrl, email, password)
        return createApiKeyOrKeepSession(baseUrl, session, deviceName)
    }

    private suspend fun signInForSession(baseUrl: String, email: String, password: String): Credential.Session {
        val body = RybbitJson.json.encodeToString(EmailSignInRequest.serializer(), EmailSignInRequest(email, password))
        val response = httpFactory.create(credential = null, origin = baseUrl).use { postJson(it, "$baseUrl/api/auth/sign-in/email", body) }
        if (!response.status.isSuccess()) {
            if (isWrongPassword(response)) throw InvalidCredentials(code = parseCode(response.text), serverMessage = null)
            throw ApiErrorMapper.map(response.status.value, response.text, retryAfterHeader = null)
        }
        val setCookies = response.headers.getAll(HttpHeaders.SetCookie).orEmpty()
        return SessionCookie.fromSetCookieHeaders(setCookies)
            ?: throw InvalidResponse(IllegalStateException("Sign-in succeeded without a session cookie"))
    }

    private suspend fun createApiKeyOrKeepSession(
        baseUrl: String,
        session: Credential.Session,
        deviceName: String,
    ): Credential = try {
        val created = httpFactory.create(session, origin = baseUrl).use {
            RybbitApi(it, baseUrl).createApiKey(keyName(deviceName))
        }
        if (created.key.isBlank()) session else Credential.ApiKey(created.key)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (rejected: Unauthorized) {
        throw rejected
    } catch (failure: ApiException) {
        session
    }

    private suspend fun postJson(http: HttpClient, url: String, body: String): RawResponse {
        val response = http.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val text = try {
            response.bodyAsText()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            throw Network(failure)
        }
        return RawResponse(response.status, response.headers, text)
    }

    private fun isWrongPassword(response: RawResponse): Boolean =
        response.status.value == HTTP_UNAUTHORIZED || parseCode(response.text) == CODE_INVALID_EMAIL_OR_PASSWORD

    private fun parseCode(text: String): String? = try {
        RybbitJson.json.decodeFromString(AuthErrorBody.serializer(), text).code
    } catch (expected: SerializationException) {
        null
    } catch (expected: IllegalArgumentException) {
        null
    }

    /** Key name shown in the Rybbit dashboard, e.g. `Rybbit Android (Pixel 8)`. */
    private fun keyName(deviceName: String): String {
        val label = deviceName.trim().ifEmpty { deviceModel }
        return "Rybbit Android ($label)"
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val CODE_INVALID_EMAIL_OR_PASSWORD = "INVALID_EMAIL_OR_PASSWORD"
    }
}
