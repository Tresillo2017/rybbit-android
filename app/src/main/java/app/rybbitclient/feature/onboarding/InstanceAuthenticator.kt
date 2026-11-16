package app.rybbitclient.feature.onboarding

import app.rybbitclient.core.auth.AuthService
import app.rybbitclient.core.network.Credential

/**
 * The auth calls the add-server and re-login flows need. [AuthServiceAuthenticator] adapts the real
 * [AuthService]; ViewModels depend on this interface so their tests never touch HTTP.
 */
interface InstanceAuthenticator {

    /** Succeeds when a Rybbit server answers at [baseUrl]. Throws an ApiException otherwise. */
    suspend fun probe(baseUrl: String)

    /** Validates [key] against [baseUrl]. Throws Unauthorized when the key is rejected. */
    suspend fun verifyApiKey(baseUrl: String, key: String)

    /** Email/password sign-in. Returns the API key the server created, or a session fallback. */
    suspend fun signInWithPassword(baseUrl: String, email: String, password: String): Credential
}

/** Production adapter. The device name is blank so AuthService labels the key with the device model. */
class AuthServiceAuthenticator(private val authService: AuthService) : InstanceAuthenticator {

    override suspend fun probe(baseUrl: String) {
        authService.probe(baseUrl)
    }

    override suspend fun verifyApiKey(baseUrl: String, key: String) {
        authService.signInWithApiKey(baseUrl, key)
    }

    override suspend fun signInWithPassword(baseUrl: String, email: String, password: String): Credential =
        authService.signInWithPassword(baseUrl, email, password, deviceName = "")
}

/** The host part of [baseUrl] for display, or null when it cannot be parsed. */
fun hostOf(baseUrl: String): String? = runCatching { java.net.URI(baseUrl).host }.getOrNull()
