package app.rybbitclient.feature.errors

import app.rybbitclient.core.model.ErrorEvent
import app.rybbitclient.core.model.ErrorName
import app.rybbitclient.core.model.ErrorTimePoint
import app.rybbitclient.core.network.Server
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorDetailViewModelTest {

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
    fun `loads the summary, time series and occurrences for the selected message`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            errorNamesResult = listOf(ERROR_NAME, ERROR_NAME.copy(value = "Other", count = 1))
            errorTimeSeriesResult = listOf(ErrorTimePoint(time = "2026-11-16 00:00:00", errorCount = 12))
            errorEventsResult = listOf(ErrorEvent(message = MESSAGE, stack = "at render (app.js:10:5)"))
        }

        // Act
        val viewModel = ErrorDetailViewModel(SITE_ID, MESSAGE, analytics, SiteQueryState())

        // Assert
        val content = contentOf(viewModel)
        assertEquals(ErrorSummary(errorName = "TypeError", count = 12, sessionCount = 4, percentage = 100.0), content.summary)
        assertEquals(1, content.points.size)
        assertEquals(listOf(MESSAGE), analytics.errorEventMessages)
        assertEquals(listOf("at render (app.js:10:5)"), content.occurrences.map { it.stack })
    }

    @Test
    fun `summary is null when the message is not in the error list`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply { errorNamesResult = listOf(ERROR_NAME.copy(value = "Other")) }

        // Act
        val viewModel = ErrorDetailViewModel(SITE_ID, MESSAGE, analytics, SiteQueryState())

        // Assert
        assertNull(contentOf(viewModel).summary)
    }

    @Test
    fun `shows the error state when a section fails`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            failure = Server(status = 500, code = null, serverMessage = null, resolution = null)
        }

        // Act
        val viewModel = ErrorDetailViewModel(SITE_ID, MESSAGE, analytics, SiteQueryState())

        // Assert
        assertTrue(viewModel.uiState.value is ErrorDetailUiState.Error)
    }

    private fun contentOf(viewModel: ErrorDetailViewModel): ErrorDetailUiState.Content =
        viewModel.uiState.value as? ErrorDetailUiState.Content ?: error("Expected Content but was ${viewModel.uiState.value}")

    private companion object {
        const val SITE_ID = "1"
        const val MESSAGE = "Cannot read properties of undefined (reading 'map')"

        val ERROR_NAME = ErrorName(
            value = MESSAGE,
            errorName = "TypeError",
            count = 12,
            sessionCount = 4,
            percentage = 100.0,
        )
    }
}
