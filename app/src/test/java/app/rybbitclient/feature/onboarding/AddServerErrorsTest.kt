package app.rybbitclient.feature.onboarding

import app.rybbitclient.core.network.Forbidden
import app.rybbitclient.core.network.InvalidCredentials
import app.rybbitclient.core.network.InvalidResponse
import app.rybbitclient.core.network.Network
import app.rybbitclient.core.network.RateLimited
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.network.Unauthorized
import java.io.IOException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertEquals
import org.junit.Test

/** Copy for add-server failures. These are pure functions, so they are tested directly. */
class AddServerErrorsTest {

    @Test
    fun `tls failure anywhere in the cause chain maps to the secure-connection copy`() {
        // Arrange
        val wrapped = Network(IOException("outer", SSLHandshakeException("inner")))

        // Act
        val message = describeAddServerError(wrapped, AddServerStage.SignIn)

        // Assert
        assertEquals(SECURE_CONNECTION_FAILED, message)
    }

    @Test
    fun `unknown host maps to a find-the-server message`() {
        // Arrange
        val error = Network(UnknownHostException("nope.example"))

        // Act
        val message = describeAddServerError(error, AddServerStage.Probe)

        // Assert
        assertEquals("Couldn't find that server. Check the address.", message)
    }

    @Test
    fun `other network failures map to a connection message`() {
        // Arrange
        val error = Network(IOException("timeout"))

        // Act
        val message = describeAddServerError(error, AddServerStage.SignIn)

        // Assert
        assertEquals("Couldn't reach the server. Check the address and your connection.", message)
    }

    @Test
    fun `any non-network failure during the probe means this is not a rybbit server`() {
        // Arrange
        val errors = listOf(
            Server(status = 500, code = null, serverMessage = null, resolution = null),
            InvalidResponse(IllegalStateException("html")),
        )

        // Act
        val messages = errors.map { describeAddServerError(it, AddServerStage.Probe) }

        // Assert
        assertEquals(listOf(NOT_A_RYBBIT_SERVER, NOT_A_RYBBIT_SERVER), messages)
    }

    @Test
    fun `sign-in failures map to specific copy`() {
        // Arrange and Act
        val wrongPassword = describeAddServerError(InvalidCredentials(code = null, serverMessage = null), AddServerStage.SignIn)
        val rejectedKey = describeAddServerError(Unauthorized(null, null, null), AddServerStage.SignIn)
        val forbidden = describeAddServerError(Forbidden(null, null, null), AddServerStage.SignIn)
        val limited = describeAddServerError(RateLimited(retryAfterSeconds = 30, code = null, serverMessage = null, resolution = null), AddServerStage.SignIn)
        val broken = describeAddServerError(Server(status = 503, code = null, serverMessage = null, resolution = null), AddServerStage.SignIn)

        // Assert
        assertEquals("The email or password is incorrect.", wrongPassword)
        assertEquals("That API key was rejected. Check that it was copied in full and hasn't been revoked.", rejectedKey)
        assertEquals("This key can't list organizations on this server.", forbidden)
        assertEquals("Too many sign-in attempts. Wait a minute and try again.", limited)
        assertEquals("The server had a problem. Try again in a few minutes.", broken)
    }
}
