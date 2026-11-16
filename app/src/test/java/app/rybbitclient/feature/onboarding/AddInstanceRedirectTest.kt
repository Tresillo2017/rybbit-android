package app.rybbitclient.feature.onboarding

import app.rybbitclient.core.storage.InMemoryKeyValueStore
import app.rybbitclient.core.storage.InMemorySecretStore
import app.rybbitclient.core.storage.InstanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A self-hosted server behind an http→https (or proxy) redirect is signed in to and stored at its real address. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddInstanceRedirectTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `sign-in uses and stores the redirect target`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator().apply {
            redirects["http://analytics.example.com"] = "https://analytics.example.com"
        }
        val repository = InstanceRepository(InMemoryKeyValueStore(), InMemorySecretStore())
        val viewModel = AddInstanceViewModel(isCloud = false, authenticator = auth, instances = repository)
        viewModel.onServerInputChanged("http://analytics.example.com")
        viewModel.onApiKeyChanged("key")

        // Act
        viewModel.submit()

        // Assert
        assertEquals(listOf("https://analytics.example.com"), auth.signInUrls)
        assertEquals("https://analytics.example.com", repository.instances.first().single().baseUrl)
    }

    @Test
    fun `check server shows the redirect target and sign-in reuses it without probing again`() = runTest(dispatcher) {
        // Arrange
        val auth = FakeInstanceAuthenticator().apply {
            redirects["http://example.com"] = "https://example.com/rybbit"
        }
        val repository = InstanceRepository(InMemoryKeyValueStore(), InMemorySecretStore())
        val viewModel = AddInstanceViewModel(isCloud = false, authenticator = auth, instances = repository)
        viewModel.onServerInputChanged("http://example.com")

        // Act
        viewModel.onCheckServer()
        viewModel.onApiKeyChanged("key")
        viewModel.submit()

        // Assert
        assertTrue(viewModel.uiState.value.previewText.orEmpty().contains("https://example.com/rybbit"))
        assertEquals(1, auth.probedUrls.size)
        assertEquals(listOf("https://example.com/rybbit"), auth.signInUrls)
    }
}
