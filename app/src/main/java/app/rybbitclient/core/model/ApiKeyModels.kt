package app.rybbitclient.core.model

import kotlinx.serialization.Serializable

/** Request body for `POST /api/user/api-keys` (session auth only). */
@Serializable
data class CreateApiKeyRequest(val name: String)

/** Response of `POST /api/user/api-keys`. [key] is shown once and must be stored as a secret. */
@Serializable
data class ApiKeyCreated(val key: String)
