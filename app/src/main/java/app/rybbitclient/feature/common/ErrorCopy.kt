package app.rybbitclient.feature.common

import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.ClientError
import app.rybbitclient.core.network.Forbidden
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.InvalidCredentials
import app.rybbitclient.core.network.InvalidResponse
import app.rybbitclient.core.network.Network
import app.rybbitclient.core.network.NotFound
import app.rybbitclient.core.network.RateLimited
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.network.Unauthorized
import app.rybbitclient.core.network.UntrustedRedirect

/** Title and body for an error state. Always written by us, never raw server text. */
data class ErrorCopy(val title: String, val message: String)

/** Friendly, localizable-later copy for every [ApiException]. Add a branch here when a new type appears. */
fun ApiException.toErrorCopy(): ErrorCopy = when (this) {
    is Unauthorized -> ErrorCopy("Sign in again", "Your access to this server has expired. Sign in again in Settings.")
    is InvalidCredentials -> ErrorCopy("Sign-in failed", "The email or password is incorrect.")
    is Forbidden -> ErrorCopy("No access", "This account can't view this data.")
    is FeatureUnavailable -> ErrorCopy("Not available on this server", "This report isn't offered by this Rybbit server.")
    is NotFound -> ErrorCopy("Not found", "That item no longer exists.")
    is RateLimited -> ErrorCopy("Too many requests", "Wait a moment and try again.")
    is ClientError -> ErrorCopy("Request rejected", "The server didn't accept this request.")
    is Server -> ErrorCopy("Server problem", "The Rybbit server failed. Try again later.")
    is Network -> ErrorCopy("No connection", "Couldn't reach the server. Check your connection.")
    is InvalidResponse -> ErrorCopy("Unexpected response", "The server sent data this app can't read yet.")
    is UntrustedRedirect -> ErrorCopy("Server moved", "This server redirects to $target. Add that address as a new server if you trust it.")
}
