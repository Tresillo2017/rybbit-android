package app.rybbitclient.feature.performance

import app.rybbitclient.core.model.PerformanceOverview
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.SiteQueryState
import app.rybbitclient.feature.common.FakeSiteAnalytics
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

@OptIn(ExperimentalCoroutinesApi::class)
class WebVitalsViewModelTest {

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
    fun `all null percentiles are the unavailable state, not an error`() {
        // Arrange: shape of docs/fixtures/performance_overview.json (self-hosted)
        val analytics = FakeSiteAnalytics(performanceHandler = { PerformanceOverview(totalPerformanceEvents = 0) })

        // Act
        val viewModel = WebVitalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        assertEquals(WebVitalsUiState.Unavailable, viewModel.uiState.value)
    }

    @Test
    fun `a feature unavailable response is the unavailable state`() {
        // Arrange
        val analytics = FakeSiteAnalytics(performanceHandler = {
            throw FeatureUnavailable(code = "API_ROUTE_NOT_FOUND", serverMessage = null, resolution = null)
        })

        // Act
        val viewModel = WebVitalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        assertEquals(WebVitalsUiState.Unavailable, viewModel.uiState.value)
    }

    @Test
    fun `readings are rated by their 75th percentile against the thresholds`() {
        // Arrange
        val overview = PerformanceOverview(
            lcpP50 = 1200.0, lcpP75 = 2400.0, lcpP90 = 3000.0, lcpP99 = 5200.0,
            clsP75 = 0.3, clsP50 = 0.02,
            totalPerformanceEvents = 512,
        )
        val analytics = FakeSiteAnalytics(performanceHandler = { overview })

        // Act
        val viewModel = WebVitalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        val content = viewModel.uiState.value as WebVitalsUiState.Content
        val lcp = content.readings.first { it.metric == VitalMetric.Lcp }
        val cls = content.readings.first { it.metric == VitalMetric.Cls }
        assertEquals(VitalRating.Good, lcp.rating)
        assertEquals(VitalRating.Poor, cls.rating)
        assertEquals(VitalRating.Good, lcp.ratingOf(lcp.p50))
        assertEquals(VitalRating.Poor, lcp.ratingOf(lcp.p99))
        assertEquals(512, content.sampleCount)
    }

    @Test
    fun `a server error is an error state`() {
        // Arrange
        val analytics = FakeSiteAnalytics(performanceHandler = { throw Server(500, null, null, null) })

        // Act
        val viewModel = WebVitalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        assertTrue(viewModel.uiState.value is WebVitalsUiState.Error)
    }

    private companion object {
        const val SITE_ID = "1"
    }
}
