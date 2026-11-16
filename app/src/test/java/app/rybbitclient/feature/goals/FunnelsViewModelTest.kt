package app.rybbitclient.feature.goals

import app.rybbitclient.core.model.Funnel
import app.rybbitclient.core.model.FunnelStep
import app.rybbitclient.core.network.Server
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
class FunnelsViewModelTest {

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
    fun `lists saved funnels as content`() {
        // Arrange
        val analytics = FakeSiteAnalytics(funnelsHandler = { listOf(funnel(1), funnel(2)) })

        // Act
        val viewModel = FunnelsViewModel(SITE_ID, analytics)

        // Assert
        val content = viewModel.uiState.value as FunnelsUiState.Content
        assertEquals(listOf(1, 2), content.funnels.map { it.id })
    }

    @Test
    fun `no saved funnels is an empty content state`() {
        // Arrange
        val analytics = FakeSiteAnalytics(funnelsHandler = { emptyList() })

        // Act
        val viewModel = FunnelsViewModel(SITE_ID, analytics)

        // Assert
        assertTrue((viewModel.uiState.value as FunnelsUiState.Content).funnels.isEmpty())
    }

    @Test
    fun `a failed request is an error state and refresh recovers`() {
        // Arrange
        var calls = 0
        val analytics = FakeSiteAnalytics(funnelsHandler = {
            calls += 1
            if (calls == 1) throw Server(status = 500, code = null, serverMessage = null, resolution = null) else listOf(funnel(9))
        })
        val viewModel = FunnelsViewModel(SITE_ID, analytics)
        val failed = viewModel.uiState.value

        // Act
        viewModel.refresh()

        // Assert
        assertTrue(failed is FunnelsUiState.Error)
        assertEquals(listOf(9), (viewModel.uiState.value as FunnelsUiState.Content).funnels.map { it.id })
    }

    private fun funnel(id: Int) = Funnel(id = id, name = "Funnel $id", steps = listOf(FunnelStep("page", "/", "Home")))

    private companion object {
        const val SITE_ID = "1"
    }
}
