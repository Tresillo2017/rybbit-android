package app.rybbitclient.feature.goals

import app.rybbitclient.core.model.Funnel
import app.rybbitclient.core.model.FunnelStep
import app.rybbitclient.core.model.FunnelStepResult
import app.rybbitclient.core.network.NotFound
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.SiteQueryState
import app.rybbitclient.core.ui.components.FunnelStepItem
import app.rybbitclient.feature.common.FakeSiteAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FunnelDetailViewModelTest {

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
    fun `analyzes the selected funnel and maps its steps and overall conversion`() {
        // Arrange
        val analytics = FakeSiteAnalytics(
            funnelsHandler = { listOf(signupFunnel()) },
            analyzeHandler = { _, _ -> SIGNUP_RESULTS },
        )

        // Act
        val viewModel = FunnelDetailViewModel(SITE_ID, FUNNEL_ID, analytics, SiteQueryState())

        // Assert
        val content = viewModel.uiState.value as FunnelDetailUiState.Content
        assertEquals("Signup funnel", content.funnelName)
        assertEquals(FunnelStepItem("Home", 8.0, 100.0, 0.0), content.analysis.steps.first())
        assertEquals(FunnelStepItem("Signup", 2.0, 25.0, 71.43), content.analysis.steps.last())
        assertEquals(25.0, content.analysis.overallConversionPercent!!, 0.0001)
    }

    @Test
    fun `an unknown funnel id is a not found error`() {
        // Arrange
        val analytics = FakeSiteAnalytics(funnelsHandler = { emptyList() })

        // Act
        val viewModel = FunnelDetailViewModel(SITE_ID, FUNNEL_ID, analytics, SiteQueryState())

        // Assert
        val error = viewModel.uiState.value as FunnelDetailUiState.Error
        assertTrue(error.error is NotFound)
    }

    @Test
    fun `a funnel with no steps shows an empty analysis without calling the analyze endpoint`() {
        // Arrange
        var analyzeCalls = 0
        val analytics = FakeSiteAnalytics(
            funnelsHandler = { listOf(Funnel(id = FUNNEL_ID, name = "Empty", steps = emptyList())) },
            analyzeHandler = { _, _ -> analyzeCalls += 1; emptyList() },
        )

        // Act
        val viewModel = FunnelDetailViewModel(SITE_ID, FUNNEL_ID, analytics, SiteQueryState())

        // Assert
        val content = viewModel.uiState.value as FunnelDetailUiState.Content
        assertTrue(content.analysis.steps.isEmpty())
        assertNull(content.analysis.overallConversionPercent)
        assertEquals(0, analyzeCalls)
    }

    @Test
    fun `changing the shared range re-analyzes the funnel with that range`() {
        // Arrange
        val seenRanges = mutableListOf<DateRange>()
        val siteQuery = SiteQueryState()
        val analytics = FakeSiteAnalytics(
            funnelsHandler = { listOf(signupFunnel()) },
            analyzeHandler = { query: AnalyticsQuery, _ -> seenRanges += query.range; SIGNUP_RESULTS },
        )
        FunnelDetailViewModel(SITE_ID, FUNNEL_ID, analytics, siteQuery)

        // Act
        siteQuery.setRange(DateRange.Last90Days)

        // Assert
        assertEquals(listOf(DateRange.Last7Days, DateRange.Last90Days), seenRanges)
    }

    @Test
    fun `a failed analysis is an error state`() {
        // Arrange
        val analytics = FakeSiteAnalytics(
            funnelsHandler = { listOf(signupFunnel()) },
            analyzeHandler = { _, _ -> throw Server(status = 500, code = null, serverMessage = null, resolution = null) },
        )

        // Act
        val viewModel = FunnelDetailViewModel(SITE_ID, FUNNEL_ID, analytics, SiteQueryState())

        // Assert
        assertTrue(viewModel.uiState.value is FunnelDetailUiState.Error)
    }

    private fun signupFunnel() = Funnel(
        id = FUNNEL_ID,
        name = "Signup funnel",
        steps = listOf(
            FunnelStep(type = "page", value = "/", name = "Home"),
            FunnelStep(type = "page", value = "/pricing", name = "Pricing"),
            FunnelStep(type = "page", value = "/signup", name = "Signup"),
        ),
    )

    private companion object {
        const val SITE_ID = "1"
        const val FUNNEL_ID = 1

        /** Shape of docs/fixtures/funnel_analyze.json. */
        val SIGNUP_RESULTS = listOf(
            FunnelStepResult(stepNumber = 1, stepName = "Home", sessions = 8, conversionRate = 100.0, dropoffRate = 0.0, visitors = 8),
            FunnelStepResult(stepNumber = 2, stepName = "Pricing", sessions = 7, conversionRate = 87.5, dropoffRate = 12.5, visitors = 7),
            FunnelStepResult(stepNumber = 3, stepName = "Signup", sessions = 2, conversionRate = 25.0, dropoffRate = 71.43, visitors = 2),
        )
    }
}
