package app.rybbitclient.core.model

import kotlinx.serialization.Serializable

/** Whether an instance is Rybbit Cloud or a self-hosted server. Drives UI copy and feature defaults. */
@Serializable
enum class InstanceKind { Cloud, SelfHosted }

/** How the stored credential authenticates. [Session] is the fallback when API-key creation fails. */
@Serializable
enum class AuthType { ApiKey, Session }

/**
 * A configured Rybbit server the user can switch between. Contains no secrets: the credential lives
 * only in the encrypted secret store, keyed by [id].
 */
@Serializable
data class Instance(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val kind: InstanceKind,
    val authType: AuthType,
    val accountLabel: String? = null,
    val createdAtMillis: Long,
) {
    /** True when the base URL is plain http://, which the UI must flag as insecure. */
    val isInsecure: Boolean get() = baseUrl.startsWith("http://")
}
