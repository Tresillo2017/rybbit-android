package app.rybbitclient.feature.onboarding

import app.rybbitclient.core.model.AuthType
import app.rybbitclient.core.model.InstanceKind
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.InvalidCredentials
import app.rybbitclient.core.network.Network
import app.rybbitclient.core.network.NotFound
import app.rybbitclient.core.network.Unauthorized
import app.rybbitclient.core.storage.InMemoryKeyValueStore
import app.rybbitclient.core.storage.InMemorySecretStore
import app.rybbitclient.core.storage.InstanceRepository
import javax.net.ssl.SSLHandshakeException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** ViewModel tests for adding a server: validation, probe, both sign-in methods and error copy. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddInstanceViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blank address reports an error and submit makes no calls`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator()
        val repository = newRepository()
        val viewModel = newViewModel(auth, repository, isCloud = false)

        // Act
        viewModel.submit()

        // Assert
        assertEquals("Enter the address of your Rybbit instance.", viewModel.uiState.value.urlError)
        assertTrue(auth.probedUrls.isEmpty())
        assertTrue(repository.instances.first().isEmpty())
    }

    @Test
    fun `malformed address shows the normalizer message`() = runTest(dispatcher) {
        // Arrange
        val viewModel = newViewModel(FakeInstanceAuthenticator(), newRepository(), isCloud = false)

        // Act
        viewModel.onServerInputChanged("https://analytics example.com")

        // Assert
        assertEquals("The address must not contain spaces.", viewModel.uiState.value.urlError)
        assertNull(viewModel.uiState.value.baseUrl)
    }

    @Test
    fun `live preview normalizes the address and flags plain http`() = runTest(dispatcher) {
        // Arrange
        val viewModel = newViewModel(FakeInstanceAuthenticator(), newRepository(), isCloud = false)

        // Act
        viewModel.onServerInputChanged(" http://192.168.1.10:3001/api/ ")

        // Assert
        val state = viewModel.uiState.value
        assertEquals("http://192.168.1.10:3001", state.baseUrl)
        assertTrue(state.isInsecure)
        assertEquals("Will connect to http://192.168.1.10:3001", state.previewText)
    }

    @Test
    fun `editing the address resets an earlier successful server check`() = runTest(dispatcher) {
        // Arrange
        val viewModel = newViewModel(FakeInstanceAuthenticator(), newRepository(), isCloud = false)
        viewModel.onServerInputChanged("analytics.example.com")
        viewModel.onCheckServer()
        assertEquals(ServerCheck.Reachable, viewModel.uiState.value.serverCheck)

        // Act
        viewModel.onServerInputChanged("other.example.com")

        // Assert
        assertEquals(ServerCheck.NotChecked, viewModel.uiState.value.serverCheck)
    }

    @Test
    fun `probe rejection shows the not-a-rybbit message and never signs in`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator(probeError = NotFound(code = null, serverMessage = null, resolution = null))
        val repository = newRepository()
        val viewModel = newViewModel(auth, repository, isCloud = false)
        viewModel.onServerInputChanged("analytics.example.com")
        viewModel.onApiKeyChanged("key")

        // Act
        viewModel.onCheckServer()
        viewModel.submit()

        // Assert
        assertEquals(ServerCheck.Failed, viewModel.uiState.value.serverCheck)
        assertEquals(NOT_A_RYBBIT_SERVER, viewModel.uiState.value.serverProblem)
        assertTrue(auth.verifiedKeys.isEmpty())
        assertTrue(repository.instances.first().isEmpty())
    }

    @Test
    fun `certificate failure during the probe shows the secure-connection message`() = runTest(dispatcher) {
        // Arrange
        val probeError = Network(SSLHandshakeException("certificate unknown"))
        val viewModel = newViewModel(FakeInstanceAuthenticator(probeError = probeError), newRepository(), isCloud = false)
        viewModel.onServerInputChanged("analytics.example.com")

        // Act
        viewModel.onCheckServer()

        // Assert
        assertEquals(SECURE_CONNECTION_FAILED, viewModel.uiState.value.serverProblem)
    }

    @Test
    fun `api key sign-in saves the instance, selects it and completes`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator()
        val repository = newRepository()
        val viewModel = newViewModel(auth, repository, isCloud = false, id = "inst-1", now = 42L)
        viewModel.onServerInputChanged("analytics.example.com")
        viewModel.onApiKeyChanged("  key-123  ")
        var completions = 0
        val collector = launch { viewModel.completed.collect { completions += 1 } }

        // Act
        viewModel.submit()

        // Assert
        assertEquals(listOf("key-123"), auth.verifiedKeys)
        val saved = repository.instances.first().single()
        assertEquals("inst-1", saved.id)
        assertEquals("analytics.example.com", saved.displayName)
        assertEquals("https://analytics.example.com", saved.baseUrl)
        assertEquals(InstanceKind.SelfHosted, saved.kind)
        assertEquals(AuthType.ApiKey, saved.authType)
        assertEquals(42L, saved.createdAtMillis)
        assertEquals("inst-1", repository.activeInstanceId.first())
        assertEquals(Credential.ApiKey("key-123"), repository.credentialFor("inst-1"))
        assertEquals(1, completions)
        assertFalse(viewModel.uiState.value.isSubmitting)
        collector.cancel()
    }

    @Test
    fun `rejected api key shows friendly copy and saves nothing`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator(keyError = Unauthorized(code = "AUTHENTICATION_REQUIRED", serverMessage = null, resolution = null))
        val repository = newRepository()
        val viewModel = newViewModel(auth, repository, isCloud = false)
        viewModel.onServerInputChanged("analytics.example.com")
        viewModel.onApiKeyChanged("wrong")

        // Act
        viewModel.submit()

        // Assert
        assertEquals(
            "That API key was rejected. Check that it was copied in full and hasn't been revoked.",
            viewModel.uiState.value.formError,
        )
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertTrue(repository.instances.first().isEmpty())
    }

    @Test
    fun `blank api key blocks submit with a validation message`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator()
        val viewModel = newViewModel(auth, newRepository(), isCloud = false)
        viewModel.onServerInputChanged("analytics.example.com")

        // Act
        viewModel.submit()

        // Assert
        assertEquals("Enter an API key.", viewModel.uiState.value.formError)
        assertTrue(auth.verifiedKeys.isEmpty())
    }

    @Test
    fun `password sign-in stores the returned api key and labels the account with the email`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator(passwordCredential = Credential.ApiKey("minted-key"))
        val repository = newRepository()
        val viewModel = newViewModel(auth, repository, isCloud = false)
        viewModel.onServerInputChanged("analytics.example.com")
        viewModel.onAuthMethodChanged(AuthMethod.Password)
        viewModel.onEmailChanged(" me@example.com ")
        viewModel.onPasswordChanged("secret")

        // Act
        viewModel.submit()

        // Assert
        assertEquals(listOf("me@example.com"), auth.passwordLogins)
        val saved = repository.instances.first().single()
        assertEquals(AuthType.ApiKey, saved.authType)
        assertEquals("me@example.com", saved.accountLabel)
        assertEquals(Credential.ApiKey("minted-key"), repository.credentialFor(saved.id))
    }

    @Test
    fun `password sign-in keeps a session credential when key creation was not possible`() = runTest(dispatcher) {
        // Arrange
        val session = Credential.Session(cookieName = "better-auth.session_token", cookieValue = "abc")
        val auth = FakeInstanceAuthenticator(passwordCredential = session)
        val repository = newRepository()
        val viewModel = newViewModel(auth, repository, isCloud = false)
        viewModel.onServerInputChanged("analytics.example.com")
        viewModel.onAuthMethodChanged(AuthMethod.Password)
        viewModel.onEmailChanged("me@example.com")
        viewModel.onPasswordChanged("secret")

        // Act
        viewModel.submit()

        // Assert
        val saved = repository.instances.first().single()
        assertEquals(AuthType.Session, saved.authType)
        assertEquals(session, repository.credentialFor(saved.id))
    }

    @Test
    fun `wrong password shows friendly copy`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator(passwordError = InvalidCredentials(code = "INVALID_EMAIL_OR_PASSWORD", serverMessage = null))
        val repository = newRepository()
        val viewModel = newViewModel(auth, repository, isCloud = false)
        viewModel.onServerInputChanged("analytics.example.com")
        viewModel.onAuthMethodChanged(AuthMethod.Password)
        viewModel.onEmailChanged("me@example.com")
        viewModel.onPasswordChanged("nope")

        // Act
        viewModel.submit()

        // Assert
        assertEquals("The email or password is incorrect.", viewModel.uiState.value.formError)
        assertTrue(repository.instances.first().isEmpty())
    }

    @Test
    fun `password flow validates the email before calling the server`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator()
        val viewModel = newViewModel(auth, newRepository(), isCloud = false)
        viewModel.onServerInputChanged("analytics.example.com")
        viewModel.onAuthMethodChanged(AuthMethod.Password)
        viewModel.onEmailChanged("not-an-email")
        viewModel.onPasswordChanged("secret")

        // Act
        viewModel.submit()

        // Assert
        assertEquals("Enter the email address of your account.", viewModel.uiState.value.formError)
        assertTrue(auth.passwordLogins.isEmpty())
    }

    @Test
    fun `network failure during sign-in shows the connection message`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator(keyError = Network(IOException("reset by peer")))
        val viewModel = newViewModel(auth, newRepository(), isCloud = false)
        viewModel.onServerInputChanged("analytics.example.com")
        viewModel.onApiKeyChanged("key")

        // Act
        viewModel.submit()

        // Assert
        assertEquals(
            "Couldn't reach the server. Check the address and your connection.",
            viewModel.uiState.value.formError,
        )
    }

    @Test
    fun `display name follows the host until the user edits it`() = runTest(dispatcher) {
        // Arrange
        val viewModel = newViewModel(FakeInstanceAuthenticator(), newRepository(), isCloud = false)

        // Act
        viewModel.onServerInputChanged("first.example.com")
        val followed = viewModel.uiState.value.displayName
        viewModel.onServerInputChanged("second.example.org")
        viewModel.onDisplayNameChanged("My server")
        viewModel.onServerInputChanged("third.example.net")

        // Assert
        assertEquals("first.example.com", followed)
        assertEquals("My server", viewModel.uiState.value.displayName)
    }

    @Test
    fun `cloud preset uses the fixed address and saves a cloud instance`() = runTest(dispatcher) {
        // Arrange
        val repository = newRepository()
        val auth = FakeInstanceAuthenticator()
        val viewModel = newViewModel(auth, repository, isCloud = true)
        viewModel.onApiKeyChanged("cloud-key")

        // Act
        viewModel.submit()

        // Assert
        val saved = repository.instances.first().single()
        assertEquals("https://app.rybbit.io", saved.baseUrl)
        assertEquals(InstanceKind.Cloud, saved.kind)
        assertEquals("Rybbit Cloud", saved.displayName)
        assertTrue(auth.probedUrls.isEmpty())
    }

    @Test
    fun `typed cloud address saves as a cloud instance from the self-hosted form`() = runTest(dispatcher) {
        // Arrange
        val repository = newRepository()
        val viewModel = newViewModel(FakeInstanceAuthenticator(), repository, isCloud = false)
        viewModel.onServerInputChanged("app.rybbit.io")
        viewModel.onApiKeyChanged("cloud-key")

        // Act
        viewModel.submit()

        // Assert
        assertEquals(InstanceKind.Cloud, repository.instances.first().single().kind)
    }

    private fun newViewModel(
        auth: FakeInstanceAuthenticator,
        repository: InstanceRepository,
        isCloud: Boolean,
        id: String = "instance-test",
        now: Long = 0L,
    ): AddInstanceViewModel = AddInstanceViewModel(
        isCloud = isCloud,
        authenticator = auth,
        instances = repository,
        newId = { id },
        now = { now },
    )

    private fun newRepository(): InstanceRepository =
        InstanceRepository(store = InMemoryKeyValueStore(), secrets = InMemorySecretStore())
}
