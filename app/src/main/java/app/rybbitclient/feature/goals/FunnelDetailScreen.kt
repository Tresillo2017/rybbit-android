package app.rybbitclient.feature.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.ui.components.FunnelChart
import app.rybbitclient.core.ui.components.KpiGrid
import app.rybbitclient.core.ui.components.KpiItem
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.SectionCard
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.format.formatPercent
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.common.SiteQueryControls
import app.rybbitclient.feature.common.SubScreenScaffold
import app.rybbitclient.feature.common.toErrorCopy

private const val DEFAULT_FUNNEL_TITLE = "Funnel"
private const val NO_VISITORS_SUMMARY = "No visitors entered this funnel in the selected period."

/**
 * One saved funnel analyzed for the shared date range: visitors per step, conversion and drop-off,
 * plus the overall conversion.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [funnelId]: Funnel.id.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunnelDetailScreen(
    siteId: String,
    funnelId: Int,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: FunnelDetailViewModel = viewModel(key = "funnel-$siteId-$funnelId") {
        FunnelDetailViewModel(siteId, funnelId, container.analyticsRepository, container.siteQueryState)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by container.siteQueryState.query.collectAsStateWithLifecycle()
    val title = (state as? FunnelDetailUiState.Content)?.funnelName ?: DEFAULT_FUNNEL_TITLE

    SubScreenScaffold(title = title, onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SiteQueryControls(query = query, siteQuery = container.siteQueryState)
            when (val current = state) {
                FunnelDetailUiState.Loading -> LoadingView(modifier = Modifier.padding(Spacing.screenGutter))
                is FunnelDetailUiState.Error -> {
                    val copy = current.error.toErrorCopy()
                    ErrorView(
                        title = copy.title,
                        message = copy.message,
                        modifier = Modifier.fillMaxSize(),
                        onRetry = viewModel::refresh,
                    )
                }
                is FunnelDetailUiState.Content -> FunnelDetailBody(current, viewModel::refresh)
            }
        }
    }
}

@Composable
private fun FunnelDetailBody(content: FunnelDetailUiState.Content, onRefresh: () -> Unit) {
    val analysis = content.analysis
    val summary = funnelSummary(analysis)
    PullToRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.sm)) {
            item(key = "conversion") {
                SectionCard(title = "Conversion", subtitle = "Visitors at each step, in the selected period") {
                    KpiGrid(items = kpiItemsFor(analysis), columns = 3)
                    FunnelChart(steps = analysis.steps, summary = summary)
                }
            }
        }
    }
}

private fun kpiItemsFor(analysis: FunnelAnalysis): List<KpiItem> = listOf(
    KpiItem(key = "entered", label = "Entered", value = analysis.enteredVisitors?.let { formatFullNumber(it.toDouble()) } ?: EMPTY_VALUE),
    KpiItem(key = "completed", label = "Completed", value = analysis.completedVisitors?.let { formatFullNumber(it.toDouble()) } ?: EMPTY_VALUE),
    KpiItem(
        key = "overall",
        label = "Overall",
        value = analysis.overallConversionPercent?.let { formatPercent(it) } ?: EMPTY_VALUE,
    ),
)

private fun funnelSummary(analysis: FunnelAnalysis): String {
    val entered = analysis.enteredVisitors?.takeIf { it > 0 } ?: return NO_VISITORS_SUMMARY
    val completed = analysis.completedVisitors ?: 0
    val percent = analysis.overallConversionPercent?.let { formatPercent(it) } ?: EMPTY_VALUE
    return "${formatFullNumber(completed.toDouble())} of ${formatFullNumber(entered.toDouble())} visitors " +
        "completed the funnel ($percent)."
}
