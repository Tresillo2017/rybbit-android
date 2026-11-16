package app.rybbitclient.feature.retention

import app.rybbitclient.core.model.RetentionCohort as CohortData
import app.rybbitclient.core.model.RetentionData
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.Server
import app.rybbitclient.feature.goals.shared.FakeSiteAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class RetentionViewModelTest {

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
    fun `cohorts are sorted oldest first and keep their sizes and null periods`() {
        // Arrange: inserted newest first, with a null for a period that has not happened yet
        val data = RetentionData(
            cohorts = linkedMapOf(
                "2026-11-16" to CohortData(size = 16, percentages = listOf(100.0)),
                "2026-11-02" to CohortData(size = 40, percentages = listOf(100.0, 30.0, null)),
            ),
        )
        val analytics = FakeSiteAnalytics(retentionHandler = { _, _ -> data })

        // Act
        val viewModel = RetentionViewModel(SITE_ID, analytics)

        // Assert
        val content = viewModel.uiState.value as RetentionUiState.Content
        assertEquals(listOf(40, 16), content.cohorts.map { it.size })
        assertEquals(listOf(100.0, 30.0, null), content.cohorts.first().percentages)
    }

    @Test
    fun `switching to weekly mode requests weekly data and keeps the range`() {
        // Arrange
        val requests = mutableListOf<Pair<String, Int>>()
        val analytics = FakeSiteAnalytics(retentionHandler = { mode, range ->
            requests += mode to range
            RetentionData()
        })
        val viewModel = RetentionViewModel(SITE_ID, analytics)

        // Act
        viewModel.selectMode(RetentionMode.Week)

        // Assert
        assertEquals(listOf("day" to DEFAULT_RETENTION_WINDOW_DAYS, "week" to DEFAULT_RETENTION_WINDOW_DAYS), requests)
        assertEquals(RetentionMode.Week, viewModel.uiState.value.mode)
        assertTrue(viewModel.uiState.value is RetentionUiState.Content)
    }

    @Test
    fun `selecting the mode that is already active does not reload`() {
        // Arrange
        var calls = 0
        val analytics = FakeSiteAnalytics(retentionHandler = { _, _ -> calls += 1; RetentionData() })
        val viewModel = RetentionViewModel(SITE_ID, analytics)

        // Act
        viewModel.selectMode(RetentionMode.Day)

        // Assert
        assertEquals(1, calls)
    }

    @Test
    fun `changing the trailing range reloads with the new window`() {
        // Arrange
        val ranges = mutableListOf<Int>()
        val analytics = FakeSiteAnalytics(retentionHandler = { _, range -> ranges += range; RetentionData() })
        val viewModel = RetentionViewModel(SITE_ID, analytics)

        // Act
        viewModel.selectRange(90)

        // Assert
        assertEquals(listOf(DEFAULT_RETENTION_WINDOW_DAYS, 90), ranges)
        assertEquals(90, viewModel.uiState.value.range)
    }

    @Test
    fun `no cohorts is an empty content state`() {
        // Arrange
        val analytics = FakeSiteAnalytics(retentionHandler = { _, _ -> RetentionData() })

        // Act
        val viewModel = RetentionViewModel(SITE_ID, analytics)

        // Assert
        assertTrue((viewModel.uiState.value as RetentionUiState.Content).cohorts.isEmpty())
    }

    @Test
    fun `a failed request keeps the selected mode and range on the error state`() {
        // Arrange
        val analytics = FakeSiteAnalytics(retentionHandler = { _, _ -> throw Server(500, null, null, null) })

        // Act
        val viewModel = RetentionViewModel(SITE_ID, analytics)

        // Assert
        val error = viewModel.uiState.value as RetentionUiState.Error
        assertEquals(RetentionMode.Day, error.mode)
        assertEquals(DEFAULT_RETENTION_WINDOW_DAYS, error.range)
    }

    @Test
    fun `switching to daily mode caps a longer weekly range at 90 days`() {
        // Arrange
        val requests = mutableListOf<Pair<String, Int>>()
        val analytics = FakeSiteAnalytics(retentionHandler = { mode, range ->
            requests += mode to range
            RetentionData()
        })
        val viewModel = RetentionViewModel(SITE_ID, analytics)
        viewModel.selectMode(RetentionMode.Week)
        viewModel.selectRange(365)

        // Act
        viewModel.selectMode(RetentionMode.Day)

        // Assert
        assertEquals("day" to 90, requests.last())
        assertEquals(90, viewModel.uiState.value.range)
    }

    @Test
    fun `daily mode offers only windows the server does not truncate`() {
        // Act and Assert
        assertEquals(listOf(30, 90), rangeOptionsFor(RetentionMode.Day))
        assertEquals(listOf(30, 90, 180, 365), rangeOptionsFor(RetentionMode.Week))
    }

    @Test
    fun `a missing retention route keeps its unavailable type for the screen`() {
        // Arrange
        val analytics = FakeSiteAnalytics(retentionHandler = { _, _ ->
            throw FeatureUnavailable(code = "API_ROUTE_NOT_FOUND", serverMessage = null, resolution = null)
        })

        // Act
        val viewModel = RetentionViewModel(SITE_ID, analytics)

        // Assert
        val error = viewModel.uiState.value as RetentionUiState.Error
        assertTrue(error.error is FeatureUnavailable)
    }

    @Test
    fun `cohort labels are readable dates and raw keys when they are not dates`() {
        // Act and Assert
        assertEquals("2 Nov 2026", cohortDateLabel("2026-11-02T00:00:00Z", Locale.ENGLISH))
        assertEquals("not-a-date", cohortDateLabel("not-a-date", Locale.ENGLISH))
    }

    private companion object {
        const val SITE_ID = "1"
    }
}
