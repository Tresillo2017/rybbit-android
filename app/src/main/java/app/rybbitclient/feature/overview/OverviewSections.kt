package app.rybbitclient.feature.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.KpiGrid
import app.rybbitclient.core.ui.components.LineChart
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.SectionCard
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.feature.common.toErrorCopy
import app.rybbitclient.core.network.ApiException

private const val KPI_COLUMNS = 2
private const val KPI_SKELETON_ROWS = 3
private const val TREND_SKELETON_ROWS = 1
private const val NO_ACTIVITY_TITLE = "No visits in this period"
private const val NO_ACTIVITY_MESSAGE = "Nothing was recorded for this range or filter. Try a wider range or remove a filter."

/** The six KPI tiles. Tapping one selects it for the trend chart; a failed load shows an inline error. */
@Composable
fun KpiSection(state: OverviewUiState, viewModel: OverviewViewModel) {
    val modifier = Modifier.padding(horizontal = Spacing.screenGutter)
    when (val kpis = state.kpis) {
        Section.Loading -> LoadingView(modifier = modifier.fillMaxWidth(), rowCount = KPI_SKELETON_ROWS)
        is Section.Failed -> InlineError(kpis.error, modifier, onRetry = viewModel::refresh)
        is Section.Loaded -> when {
            kpis.value.current.hasNoActivity() -> EmptyView(
                title = NO_ACTIVITY_TITLE,
                message = NO_ACTIVITY_MESSAGE,
                modifier = modifier.fillMaxWidth(),
            )
            else -> KpiGrid(
                items = kpiItems(kpis.value),
                modifier = modifier,
                selectedKey = state.selectedKpi.name,
                columns = KPI_COLUMNS,
                onItemClick = { key -> KpiMetric.entries.firstOrNull { it.name == key }?.let(viewModel::selectKpi) },
            )
        }
    }
}

/** The trend chart for the selected KPI, with the previous period dashed when the range has one. */
@Composable
fun TrendSection(state: OverviewUiState, viewModel: OverviewViewModel) {
    val metric = state.selectedKpi
    val modifier = Modifier.padding(horizontal = Spacing.screenGutter)
    val trend = state.trend
    SectionCard(
        title = "${metric.label} over time",
        modifier = modifier,
        subtitle = (trend as? Section.Loaded)?.value?.previous?.takeIf { it.isNotEmpty() }?.let { "Dashed line: previous period" },
    ) {
        when (trend) {
            Section.Loading -> LoadingView(modifier = Modifier.fillMaxWidth(), rowCount = TREND_SKELETON_ROWS)
            is Section.Failed -> InlineErrorBody(trend.error, onRetry = viewModel::refresh)
            is Section.Loaded -> TrendChart(trend.value, metric)
        }
    }
}

@Composable
private fun TrendChart(data: TrendData, metric: KpiMetric) {
    if (data.current.isEmpty()) {
        EmptyView(title = "No trend yet", message = "Visits over this range will plot here.")
        return
    }
    val labels = remember(data.bucket, data.current) { axisLabels(data.bucket, data.current.map { it.time }) }
    LineChart(
        series = chartSeries(data, metric),
        xLabelFormatter = { index -> labels.getOrElse(index) { "" } },
        valueFormatter = { value -> metric.format(value) },
    )
}

@Composable
private fun InlineError(error: ApiException, modifier: Modifier, onRetry: () -> Unit) {
    SectionCard(title = "Couldn't load", modifier = modifier) {
        InlineErrorBody(error, onRetry)
    }
}

@Composable
private fun InlineErrorBody(error: ApiException, onRetry: () -> Unit) {
    val copy = error.toErrorCopy()
    Column(modifier = Modifier.fillMaxWidth()) {
        ErrorView(title = copy.title, message = copy.message, onRetry = onRetry)
    }
}

private fun app.rybbitclient.core.model.Overview.hasNoActivity(): Boolean =
    (sessions ?: 0) == 0 && (pageviews ?: 0) == 0
