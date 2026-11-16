package app.rybbitclient.core.auth

import app.rybbitclient.core.network.Credential

/**
 * Picks the better-auth session cookie out of `Set-Cookie` header values. The cookie is
 * `__Secure-better-auth.session_token` in production and `better-auth.session_token` otherwise,
 * so the name is taken from the header rather than hardcoded.
 */
object SessionCookie {

    private const val SESSION_TOKEN_MARKER = "session_token"

    /** Returns the first `name=value` pair whose name contains `session_token`, or null. */
    fun fromSetCookieHeaders(headers: List<String>): Credential.Session? {
        return headers.asSequence()
            .map { it.substringBefore(';').trim() }
            .filter { it.contains('=') }
            .map { pair -> pair.substringBefore('=').trim() to pair.substringAfter('=').trim() }
            .firstOrNull { (name, value) -> name.contains(SESSION_TOKEN_MARKER) && value.isNotEmpty() }
            ?.let { (name, value) -> Credential.Session(cookieName = name, cookieValue = value) }
    }
}
