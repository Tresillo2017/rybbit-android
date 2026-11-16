package app.rybbitclient.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/** Error body shape: `{"error","code","message","resolution"}`, plus `retryAfter` on 429. */
@Serializable
private data class ErrorBody(
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
    val resolution: String? = null,
    @Serializable(with = LenientLongSerializer::class)
    val retryAfter: Long? = null,
)

/** Maps an HTTP error response onto the [ApiException] hierarchy. Pure function, unit tested. */
object ApiErrorMapper {

    const val CODE_ROUTE_NOT_FOUND = "API_ROUTE_NOT_FOUND"

    fun map(status: Int, body: String, retryAfterHeader: String?): ApiException {
        val parsed = parseBody(body)
        val code = parsed?.code
        val serverMessage = parsed?.message ?: parsed?.error
        val resolution = parsed?.resolution
        return when {
            status == 401 -> Unauthorized(code, serverMessage, resolution)
            status == 403 -> Forbidden(code, serverMessage, resolution)
            status == 404 && code == CODE_ROUTE_NOT_FOUND -> FeatureUnavailable(code, serverMessage, resolution)
            status == 404 -> NotFound(code, serverMessage, resolution)
            status == 429 -> RateLimited(
                retryAfterSeconds = retryAfterHeader?.trim()?.toLongOrNull() ?: parsed?.retryAfter,
                code = code,
                serverMessage = serverMessage,
                resolution = resolution,
            )
            status in CLIENT_ERROR_RANGE -> ClientError(status, code, serverMessage, resolution)
            else -> Server(status, code, serverMessage, resolution)
        }
    }

    /** Non-JSON bodies (reverse-proxy HTML pages, for example) are expected; they fall back to status only. */
    private fun parseBody(body: String): ErrorBody? {
        if (body.isBlank()) return null
        return try {
            RybbitJson.json.decodeFromString(ErrorBody.serializer(), body)
        } catch (expected: SerializationException) {
            null
        } catch (expected: IllegalArgumentException) {
            null
        }
    }

    private val CLIENT_ERROR_RANGE = 400..499
}
