package app.rybbitclient.feature.onboarding

import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.Credential

/**
 * Scripted [InstanceAuthenticator]. Set an error to make the matching call throw; every call is recorded so
 * tests can assert which steps ran.
 */
class FakeInstanceAuthenticator(
    var probeError: ApiException? = null,
    var keyError: ApiException? = null,
    var passwordError: ApiException? = null,
    var passwordCredential: Credential = Credential.Session(cookieName = "better-auth.session_token", cookieValue = "session"),
) : InstanceAuthenticator {

    val probedUrls = mutableListOf<String>()
    val verifiedKeys = mutableListOf<String>()
    val passwordLogins = mutableListOf<String>()

    override suspend fun probe(baseUrl: String) {
        probedUrls += baseUrl
        probeError?.let { throw it }
    }

    override suspend fun verifyApiKey(baseUrl: String, key: String) {
        verifiedKeys += key
        keyError?.let { throw it }
    }

    override suspend fun signInWithPassword(baseUrl: String, email: String, password: String): Credential {
        passwordLogins += email
        passwordError?.let { throw it }
        return passwordCredential
    }
}
