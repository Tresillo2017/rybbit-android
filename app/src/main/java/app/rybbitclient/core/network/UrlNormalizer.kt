package app.rybbitclient.core.network

import java.net.URI
import java.net.URISyntaxException

/** Thrown when a user-typed instance URL cannot be turned into a valid Rybbit base URL. */
class InvalidUrlException(message: String) : IllegalArgumentException(message)

/** A validated Rybbit base URL. [baseUrl] has no trailing slash and no `/api` suffix. */
data class NormalizedBaseUrl(val baseUrl: String, val isInsecure: Boolean)

object UrlNormalizer {

    const val CLOUD_BASE_URL = "https://app.rybbit.io"

    private const val DEFAULT_SCHEME = "https"
    private const val API_SUFFIX = "/api"
    private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private val allowedSchemes = setOf("http", "https")

    /**
     * Normalizes user input such as `analytics.example.com`, `https://x.com/api/` or
     * `http://192.168.1.10:3001` into a base URL. Throws [InvalidUrlException] on bad input.
     */
    fun normalize(raw: String): NormalizedBaseUrl {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) throw InvalidUrlException("Enter the address of your Rybbit instance.")
        val withScheme = if (schemeRegex.containsMatchIn(trimmed)) trimmed else "$DEFAULT_SCHEME://$trimmed"
        val uri = parse(withScheme)
        val scheme = uri.scheme.lowercase()
        if (scheme !in allowedSchemes) throw InvalidUrlException("Only http:// and https:// addresses are supported.")
        val host = uri.host?.lowercase() ?: throw InvalidUrlException("The address is missing a host name.")
        rejectUnsupportedParts(uri)
        val port = if (uri.port == -1) "" else ":${uri.port}"
        val path = normalizePath(uri.rawPath.orEmpty())
        return NormalizedBaseUrl(
            baseUrl = "$scheme://$host$port$path",
            isInsecure = scheme == "http",
        )
    }

    private fun parse(candidate: String): URI {
        if (candidate.any { it.isWhitespace() }) {
            throw InvalidUrlException("The address must not contain spaces.")
        }
        return try {
            URI(candidate)
        } catch (e: URISyntaxException) {
            throw InvalidUrlException("That doesn't look like a valid address.").also { it.initCause(e) }
        }
    }

    private fun rejectUnsupportedParts(uri: URI) {
        if (uri.rawUserInfo != null) throw InvalidUrlException("Remove the user name and password from the address.")
        if (uri.rawQuery != null || uri.rawFragment != null) {
            throw InvalidUrlException("The address must not contain a query string or fragment.")
        }
    }

    private fun normalizePath(rawPath: String): String {
        var path = rawPath.trimEnd('/')
        if (path == API_SUFFIX) return ""
        if (path.endsWith(API_SUFFIX)) path = path.removeSuffix(API_SUFFIX).trimEnd('/')
        return path
    }
}
