package app.rybbitclient.feature.settings

import app.rybbitclient.core.model.AuthType
import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.model.InstanceKind
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.Unauthorized
import app.rybbitclient.core.storage.InMemoryKeyValueStore
import app.rybbitclient.core.storage.InMemorySecretStore
import app.rybbitclient.core.storage.InstanceRepository
import app.rybbitclient.feature.onboarding.FakeInstanceAuthenticator
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** ViewModel tests for Settings: switching, renaming, re-authenticating and removing servers. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `switching makes the chosen server active`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("one"), instance("two"))
        val viewModel = SettingsViewModel(repository, FakeInstanceAuthenticator(), remover = {})
        assertEquals("one", (viewModel.uiState.value as SettingsUiState.Content).activeInstanceId)

        // Act
        viewModel.switchTo("two")

        // Assert
        assertEquals("two", (viewModel.uiState.value as SettingsUiState.Content).activeInstanceId)
        assertEquals("two", repository.activeInstanceId.first())
    }

    @Test
    fun `rename trims the name and saves it`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("one"))
        val viewModel = SettingsViewModel(repository, FakeInstanceAuthenticator(), remover = {})

        // Act
        viewModel.rename("one", "  Main analytics  ")

        // Assert
        assertEquals("Main analytics", repository.instance("one")?.displayName)
    }

    @Test
    fun `blank rename is ignored`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("one"))
        val viewModel = SettingsViewModel(repository, FakeInstanceAuthenticator(), remover = {})

        // Act
        viewModel.rename("one", "   ")

        // Assert
        assertEquals("Server one", repository.instance("one")?.displayName)
    }

    @Test
    fun `removing one of two servers keeps the app on settings`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("one"), instance("two"))
        val removed = mutableListOf<String>()
        val viewModel = SettingsViewModel(repository, FakeInstanceAuthenticator(), remover = { id ->
            removed += id
            repository.remove(id)
        })
        var leftForOnboarding = 0
        val collector = launch { viewModel.allInstancesRemoved.collect { leftForOnboarding += 1 } }

        // Act
        viewModel.remove("one")

        // Assert
        assertEquals(listOf("one"), removed)
        assertEquals(listOf("two"), repository.instances.first().map { it.id })
        assertEquals(0, leftForOnboarding)
        collector.cancel()
    }

    @Test
    fun `removing the last server signals that onboarding should open`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("only"))
        val viewModel = SettingsViewModel(repository, FakeInstanceAuthenticator(), remover = { id -> repository.remove(id) })
        var leftForOnboarding = 0
        val collector = launch { viewModel.allInstancesRemoved.collect { leftForOnboarding += 1 } }

        // Act
        viewModel.remove("only")

        // Assert
        assertTrue(repository.instances.first().isEmpty())
        assertEquals(1, leftForOnboarding)
        collector.cancel()
    }

    @Test
    fun `reauthenticate stores the verified key and runs the callback`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("one"))
        val auth = FakeInstanceAuthenticator()
        val viewModel = SettingsViewModel(repository, auth, remover = {})
        var done = false

        // Act
        viewModel.reauthenticate("one", "  new-key  ") { done = true }

        // Assert
        assertEquals(listOf("new-key"), auth.verifiedKeys)
        assertEquals(Credential.ApiKey("new-key"), repository.credentialFor("one"))
        assertEquals(AuthType.ApiKey, repository.instance("one")?.authType)
        assertTrue(done)
        assertNull((viewModel.uiState.value as SettingsUiState.Content).errorMessage)
    }

    @Test
    fun `rejected key keeps the old credential and reports the error`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("one"))
        val auth = FakeInstanceAuthenticator(keyError = Unauthorized(code = null, serverMessage = null, resolution = null))
        val viewModel = SettingsViewModel(repository, auth, remover = {})
        var done = false

        // Act
        viewModel.reauthenticate("one", "bad-key") { done = true }

        // Assert
        assertEquals(Credential.ApiKey("key-one"), repository.credentialFor("one"))
        assertTrue(!done)
        val content = viewModel.uiState.value as SettingsUiState.Content
        assertEquals(
            "That API key was rejected. Check that it was copied in full and hasn't been revoked.",
            content.errorMessage,
        )
        assertEquals(false, content.isWorking)
    }

    @Test
    fun `blank key is rejected before any request`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("one"))
        val auth = FakeInstanceAuthenticator()
        val viewModel = SettingsViewModel(repository, auth, remover = {})

        // Act
        viewModel.reauthenticate("one", "   ") {}

        // Assert
        assertTrue(auth.verifiedKeys.isEmpty())
        assertEquals("Enter an API key.", (viewModel.uiState.value as SettingsUiState.Content).errorMessage)
    }

    private fun instance(id: String) = Instance(
        id = id,
        displayName = "Server $id",
        baseUrl = "https://$id.example.com",
        kind = InstanceKind.SelfHosted,
        authType = AuthType.ApiKey,
        createdAtMillis = 0L,
    )

    private suspend fun repositoryWith(vararg instances: Instance): InstanceRepository {
        val repository = InstanceRepository(store = InMemoryKeyValueStore(), secrets = InMemorySecretStore())
        instances.forEach { repository.add(it, Credential.ApiKey("key-${it.id}")) }
        return repository
    }
}
