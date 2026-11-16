package app.rybbitclient.feature.errors

import app.rybbitclient.core.model.ErrorName
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.SiteQueryState
import app.rybbitclient.feature.events.FakeAnalyticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorsViewModelTest {

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
    fun `loads the distinct error messages with their counts`() = runTest(dispatcher) {
        // Arrange
        val names = listOf(ERROR_NAME)
        val analytics = FakeAnalyticsRepository().apply { errorNamesResult = names }

        // Act
        val viewModel = ErrorsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        val content = viewModel.uiState.value as ErrorsUiState.Content
        assertEquals(names, content.errors)
    }

    @Test
    fun `an empty period is content with no errors, not an error`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository()

        // Act
        val viewModel = ErrorsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        val content = viewModel.uiState.value as ErrorsUiState.Content
        assertTrue(content.errors.isEmpty())
    }

    @Test
    fun `changing the shared site query reloads the list`() = runTest(dispatcher) {
        // Arrange
        val queryState = SiteQueryState()
        val viewModel = ErrorsViewModel(SITE_ID, FakeAnalyticsRepository(), queryState)

        // Act
        queryState.setRange(DateRange.Last90Days)

        // Assert
        assertEquals(DateRange.Last90Days, (viewModel.uiState.value as ErrorsUiState.Content).query.range)
    }

    @Test
    fun `shows the error state when the request fails`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            failure = Server(status = 500, code = null, serverMessage = null, resolution = null)
        }

        // Act
        val viewModel = ErrorsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        assertTrue(viewModel.uiState.value is ErrorsUiState.Error)
    }

    @Test
    fun `reports unavailable when the server lacks error reports`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            failure = FeatureUnavailable(code = "API_ROUTE_NOT_FOUND", serverMessage = null, resolution = null)
        }

        // Act
        val viewModel = ErrorsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        assertEquals(ErrorsUiState.Unavailable, viewModel.uiState.value)
    }

    private companion object {
        const val SITE_ID = "1"

        val ERROR_NAME = ErrorName(
            value = "Cannot read properties of undefined (reading 'map')",
            errorName = "TypeError",
            count = 12,
            sessionCount = 4,
            percentage = 100.0,
        )
    }
}
