package app.rybbitclient.feature.errors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.model.ErrorEvent
import app.rybbitclient.core.model.ErrorTimePoint
import app.rybbitclient.core.query.TimeBucket
import app.rybbitclient.core.ui.components.KpiGrid
import app.rybbitclient.core.ui.components.KpiItem
import app.rybbitclient.core.ui.components.LineChart
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.SectionCard
import app.rybbitclient.core.ui.components.UnavailableView
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.format.formatPercent
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.events.ApiErrorView
import app.rybbitclient.feature.events.DetailScaffold
import app.rybbitclient.feature.events.SiteQueryControls
import app.rybbitclient.feature.events.buildTimelineChart
import app.rybbitclient.feature.events.errorTimeline

private const val SCREEN_TITLE = "Error details"
private const val ERRORS_SUBJECT = "Errors"
private const val OCCURRENCES_TITLE = "Occurrences"
private const val NO_OCCURRENCES_MESSAGE = "No occurrences in this period."
private const val KPI_COLUMNS = 3

/**
 * Detail for one error message: totals, occurrences over time, and each occurrence with its page,
 * device and expandable stack trace. Tapping "View session" opens that session.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [errorMessage]: Error message, exactly as grouped by the server. Sent as the camelCase `errorMessage` parameter.
 * - [onOpenSession]: Open SessionDetail for an occurrence's session id.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorDetailScreen(
    siteId: String,
    errorMessage: String,
    onOpenSession: (sessionId: String) -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ErrorDetailViewModel = viewModel(key = "error-$siteId-$errorMessage") {
        ErrorDetailViewModel(siteId, errorMessage, container.analyticsRepository, container.siteQueryState)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by container.siteQueryState.query.collectAsStateWithLifecycle()

    DetailScaffold(title = SCREEN_TITLE, onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SiteQueryControls(
                queryState = container.siteQueryState,
                query = query,
                modifier = Modifier.padding(vertical = Spacing.sm),
            )
            when (val current = state) {
                ErrorDetailUiState.Loading -> LoadingView(modifier = Modifier.fillMaxSize())
                is ErrorDetailUiState.Error -> ApiErrorView(
                    error = current.error,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                )
                ErrorDetailUiState.Unavailable -> UnavailableView(modifier = Modifier.fillMaxSize())
                is ErrorDetailUiState.Content -> ErrorDetailList(
                    content = current,
                    errorMessage = errorMessage,
                    onRefresh = viewModel::refresh,
                    onOpenSession = onOpenSession,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorDetailList(
    content: ErrorDetailUiState.Content,
    errorMessage: String,
    onRefresh: () -> Unit,
    onOpenSession: (String) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { ErrorMessageHeader(errorMessage = errorMessage) }
            content.summary?.let { summary ->
                item { ErrorSummaryGrid(summary = summary) }
            }
            item { ErrorChartCard(points = content.points, bucket = content.bucket) }
            item {
                Text(
                    text = OCCURRENCES_TITLE,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (content.occurrences.isEmpty()) {
                item {
                    Text(
                        text = NO_OCCURRENCES_MESSAGE,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(content.occurrences, key = { index, occurrence -> "$index:${occurrence.timestamp}:${occurrence.sessionId}" }) { _, occurrence: ErrorEvent ->
                ErrorOccurrenceCard(occurrence = occurrence, onOpenSession = onOpenSession)
            }
        }
    }
}

@Composable
private fun ErrorMessageHeader(errorMessage: String) {
    SelectionContainer(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ErrorSummaryGrid(summary: ErrorSummary) {
    val share = summary.percentage?.let { formatPercent(it) } ?: EMPTY_VALUE
    KpiGrid(
        items = listOf(
            KpiItem(key = "occurrences", label = "Occurrences", value = formatFullNumber(summary.count.toDouble())),
            KpiItem(key = "sessions", label = "Sessions", value = formatFullNumber(summary.sessionCount.toDouble())),
            KpiItem(key = "share", label = "Share of errors", value = share),
        ),
        columns = KPI_COLUMNS,
    )
}

@Composable
private fun ErrorChartCard(points: List<ErrorTimePoint>, bucket: TimeBucket) {
    val chart = remember(points, bucket) {
        buildTimelineChart(errorTimeline(points), bucket, subject = ERRORS_SUBJECT)
    }
    SectionCard(title = "Occurrences over time", subtitle = "Count per bucket") {
        LineChart(
            series = chart.series,
            summary = chart.summary,
            xLabelFormatter = { index -> chart.labels.getOrElse(index) { "" } },
        )
    }
}
