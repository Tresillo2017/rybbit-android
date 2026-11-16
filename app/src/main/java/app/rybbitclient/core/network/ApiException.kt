package app.rybbitclient.core.network

/**
 * Every failure a Rybbit request can produce. UI code maps these to friendly copy and never
 * shows raw server text as a title. HTTP-status errors keep the parsed server body for diagnostics.
 */
sealed class ApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** An HTTP error response. [code] is the machine code from the error body, e.g. `AUTHENTICATION_REQUIRED`. */
sealed class HttpStatusException(
    val status: Int,
    val code: String?,
    val serverMessage: String?,
    val resolution: String?,
    message: String,
) : ApiException(message)

/** 401: the credential is missing, expired or revoked. */
class Unauthorized(
    code: String?,
    serverMessage: String?,
    resolution: String?,
) : HttpStatusException(401, code, serverMessage, resolution, "Your sign-in is no longer valid.")

/** 403: the credential is valid but lacks access to the site or scope. */
class Forbidden(
    code: String?,
    serverMessage: String?,
    resolution: String?,
) : HttpStatusException(403, code, serverMessage, resolution, "This account can't access that resource.")

/** 404 with code `API_ROUTE_NOT_FOUND`: the endpoint does not exist on this Rybbit build (e.g. cloud-only). */
class FeatureUnavailable(
    code: String?,
    serverMessage: String?,
    resolution: String?,
) : HttpStatusException(404, code, serverMessage, resolution, "This feature isn't available on this server.")

/** 404 for a missing resource (site, session, user, goal...). */
class NotFound(
    code: String?,
    serverMessage: String?,
    resolution: String?,
) : HttpStatusException(404, code, serverMessage, resolution, "The requested item doesn't exist.")

/** 429: rate limited. [retryAfterSeconds] comes from the `Retry-After` header or body when present. */
class RateLimited(
    val retryAfterSeconds: Long?,
    code: String?,
    serverMessage: String?,
    resolution: String?,
) : HttpStatusException(429, code, serverMessage, resolution, "Too many requests. Try again shortly.")

/** Any other 4xx response (e.g. 400 for a bad filter). */
class ClientError(
    status: Int,
    code: String?,
    serverMessage: String?,
    resolution: String?,
) : HttpStatusException(status, code, serverMessage, resolution, "The server rejected the request ($status).")

/** 5xx response. */
class Server(
    status: Int,
    code: String?,
    serverMessage: String?,
    resolution: String?,
) : HttpStatusException(status, code, serverMessage, resolution, "The server failed to process the request ($status).")

/** Login with an email/password was rejected by the server. */
class InvalidCredentials(
    code: String?,
    serverMessage: String?,
) : HttpStatusException(401, code, serverMessage, null, "The email or password is incorrect.")

/** Connection, DNS, TLS or timeout failure: the server could not be reached. */
class Network(cause: Throwable) : ApiException("Couldn't reach the server. Check your connection.", cause)

/** The server answered, but the body could not be decoded into the expected model. */
class InvalidResponse(cause: Throwable) : ApiException("The server sent a response this app doesn't understand.", cause)
