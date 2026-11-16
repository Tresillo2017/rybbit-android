package app.rybbitclient.feature.onboarding

import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.Forbidden
import app.rybbitclient.core.network.InvalidCredentials
import app.rybbitclient.core.network.InvalidResponse
import app.rybbitclient.core.network.Network
import app.rybbitclient.core.network.UntrustedRedirect
import app.rybbitclient.core.network.RateLimited
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.network.Unauthorized
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Which step of adding a server failed: reaching it, or signing in to it. */
enum class AddServerStage { Probe, SignIn }

internal const val NOT_A_RYBBIT_SERVER = "No Rybbit server answered at this address. Check the address and try again."
internal const val SECURE_CONNECTION_FAILED =
    "The secure connection failed. The server's certificate may be invalid or untrusted. " +
        "Check its HTTPS setup, or install the certificate authority on this device."

/** Friendly copy for add-server and re-login failures. Never shows raw server text. */
fun describeAddServerError(error: ApiException, stage: AddServerStage): String = when {
    error is Network -> describeNetworkError(error)
    error is UntrustedRedirect -> error.message.orEmpty()
    stage == AddServerStage.Probe -> NOT_A_RYBBIT_SERVER
    error is InvalidCredentials -> "The email or password is incorrect."
    error is Unauthorized -> "That API key was rejected. Check that it was copied in full and hasn't been revoked."
    error is Forbidden -> "This key can't list organizations on this server."
    error is RateLimited -> "Too many sign-in attempts. Wait a minute and try again."
    error is Server -> "The server had a problem. Try again in a few minutes."
    error is InvalidResponse -> NOT_A_RYBBIT_SERVER
    else -> "Sign-in failed. Try again."
}

private fun describeNetworkError(error: Network): String {
    val causes = generateSequence<Throwable>(error) { it.cause }.toList()
    return when {
        causes.any { it is SSLException } -> SECURE_CONNECTION_FAILED
        causes.any { it is UnknownHostException } -> "Couldn't find that server. Check the address."
        else -> "Couldn't reach the server. Check the address and your connection."
    }
}
