package app.rybbitclient.feature.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.SectionCard
import app.rybbitclient.core.ui.components.UnavailableView
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.metricValue
import app.rybbitclient.core.ui.theme.rybbitColors
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.common.SiteQueryControls
import app.rybbitclient.feature.common.SubScreenScaffold
import app.rybbitclient.feature.common.toErrorCopy

private const val UNAVAILABLE_TITLE = "Web Vitals aren't available"
private const val UNAVAILABLE_MESSAGE =
    "This server doesn't report Web Vitals, or no page performance data has been collected for this period yet."
private const val NO_VALUE_DESCRIPTION = "no data"

/**
 * Web Vitals for the site: LCP, INP, CLS, FCP and TTFB with p50, p75, p90 and p99, each coloured by
 * the published good, needs-improvement and poor thresholds (rated on p75). Shows an unavailable state
 * when the server has no performance data at all.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebVitalsScreen(
    siteId: String,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: WebVitalsViewModel = viewModel(key = "web-vitals-$siteId") {
        WebVitalsViewModel(siteId, container.analyticsRepository, container.siteQueryState)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by container.siteQueryState.query.collectAsStateWithLifecycle()

    SubScreenScaffold(title = "Web Vitals", onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SiteQueryControls(query = query, siteQuery = container.siteQueryState)
            when (val current = state) {
                WebVitalsUiState.Loading -> LoadingView(modifier = Modifier.padding(Spacing.screenGutter))
                WebVitalsUiState.Unavailable -> UnavailableView(
                    modifier = Modifier.fillMaxSize(),
                    title = UNAVAILABLE_TITLE,
                    message = UNAVAILABLE_MESSAGE,
                )
                is WebVitalsUiState.Error -> {
                    val copy = current.error.toErrorCopy()
                    ErrorView(
                        title = copy.title,
                        message = copy.message,
                        modifier = Modifier.fillMaxSize(),
                        onRetry = viewModel::refresh,
                    )
                }
                is WebVitalsUiState.Content -> VitalsBody(current, viewModel::refresh)
            }
        }
    }
}

@Composable
private fun VitalsBody(content: WebVitalsUiState.Content, onRefresh: () -> Unit) {
    PullToRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.sm)) {
            items(content.readings, key = { it.metric.name }) { reading ->
                VitalCard(reading = reading, modifier = Modifier.padding(bottom = Spacing.sm))
            }
            content.sampleCount?.let { count ->
                item(key = "samples") {
                    Text(
                        text = "Based on ${formatFullNumber(count.toDouble())} page loads in the selected period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun VitalCard(reading: VitalReading, modifier: Modifier = Modifier) {
    val metric = reading.metric
    val p75Text = formatVitalValue(metric, reading.p75)
    val ratingLabel = reading.rating?.label ?: NO_VALUE_DESCRIPTION
    val summary = "${metric.title}, 75th percentile $p75Text, $ratingLabel"
    SectionCard(
        title = "${metric.shortName} · ${metric.title}",
        subtitle = metric.description,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = summary },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text = p75Text, style = MaterialTheme.typography.metricValue, color = reading.rating.color())
            Text(text = ratingLabel, style = MaterialTheme.typography.labelLarge, color = reading.rating.color())
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PercentileCell(label = "p50", value = reading.p50, reading = reading)
            PercentileCell(label = "p75", value = reading.p75, reading = reading)
            PercentileCell(label = "p90", value = reading.p90, reading = reading)
            PercentileCell(label = "p99", value = reading.p99, reading = reading)
        }
    }
}

@Composable
private fun PercentileCell(label: String, value: Double?, reading: VitalReading) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = formatVitalValue(reading.metric, value),
            style = MaterialTheme.typography.bodyMedium,
            color = reading.ratingOf(value).color(),
        )
    }
}

@Composable
private fun VitalRating?.color(): Color = when (this) {
    VitalRating.Good -> MaterialTheme.rybbitColors.positive
    VitalRating.NeedsImprovement -> MaterialTheme.colorScheme.tertiary
    VitalRating.Poor -> MaterialTheme.rybbitColors.negative
    null -> MaterialTheme.colorScheme.onSurfaceVariant
}
