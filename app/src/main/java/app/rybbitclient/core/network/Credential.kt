package app.rybbitclient.core.network

import kotlinx.serialization.Serializable

/**
 * How a request is authenticated against one instance. Secrets never appear in [toString],
 * so accidental logging of a credential cannot leak it.
 */
@Serializable
sealed interface Credential {

    /** Sent as `Authorization: Bearer <key>`. Works on Cloud and self-hosted. */
    @Serializable
    data class ApiKey(val key: String) : Credential {
        override fun toString(): String = "Credential.ApiKey(***)"
    }

    /**
     * A better-auth session cookie captured from `Set-Cookie`, sent manually as `Cookie: name=value`.
     * Cookie jars drop `Secure` cookies on http://, so the cookie is never stored by a jar.
     */
    @Serializable
    data class Session(val cookieName: String, val cookieValue: String) : Credential {
        override fun toString(): String = "Credential.Session($cookieName=***)"
    }
}
