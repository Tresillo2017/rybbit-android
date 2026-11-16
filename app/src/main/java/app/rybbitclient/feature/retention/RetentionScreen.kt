package app.rybbitclient.feature.retention

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import app.rybbitclient.core.ui.components.ExpressiveRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.RetentionGrid
import app.rybbitclient.core.ui.components.SectionCard
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.goals.shared.ApiErrorContent
import app.rybbitclient.feature.goals.shared.SubScreenScaffold

private const val EMPTY_RETENTION_TITLE = "No returning visitors yet"
private const val EMPTY_RETENTION_MESSAGE =
    "Retention appears once visitors come back after their first visit. Try a longer range or check back later."
private const val RANGE_SUFFIX = " days"

/**
 * Cohort retention heatmap. Rows are groups of visitors by first-visit date, columns are the periods
 * after that first visit. Mode (daily or weekly) and trailing range are chosen on the screen.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetentionScreen(
    siteId: String,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: RetentionViewModel = viewModel(key = "retention-$siteId") {
        RetentionViewModel(siteId, container.analyticsRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SubScreenScaffold(title = "Retention", onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            RetentionControls(
                mode = state.mode,
                range = state.range,
                onModeSelected = viewModel::selectMode,
                onRangeSelected = viewModel::selectRange,
            )
            when (val current = state) {
                is RetentionUiState.Loading -> LoadingView(modifier = Modifier.padding(Spacing.screenGutter))
                is RetentionUiState.Error -> ApiErrorContent(
                    error = current.error,
                    modifier = Modifier.fillMaxSize(),
                    onRetry = viewModel::refresh,
                )
                is RetentionUiState.Content -> RetentionBody(content = current, onRefresh = viewModel::refresh)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetentionControls(
    mode: RetentionMode,
    range: Int,
    onModeSelected: (RetentionMode) -> Unit,
    onRangeSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenGutter)) {
            RetentionMode.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == mode,
                    onClick = { onModeSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = RetentionMode.entries.size),
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    label = { Text(option.label) },
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.screenGutter),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(rangeOptionsFor(mode), key = { it }) { days ->
                FilterChip(
                    selected = days == range,
                    onClick = { onRangeSelected(days) },
                    label = { Text("$days$RANGE_SUFFIX") },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                )
            }
        }
    }
}

@Composable
private fun RetentionBody(content: RetentionUiState.Content, onRefresh: () -> Unit) {
    ExpressiveRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.sm)) {
            if (content.cohorts.isEmpty()) {
                item(key = "empty") { EmptyView(title = EMPTY_RETENTION_TITLE, message = EMPTY_RETENTION_MESSAGE) }
            } else {
                item(key = "grid") {
                    SectionCard(
                        title = "Cohorts",
                        subtitle = "Share of each group that came back, by ${content.mode.periodNoun.lowercase()}",
                    ) {
                        RetentionGrid(
                            cohorts = content.cohorts,
                            periodLabel = { index -> "${content.mode.periodNoun} $index" },
                        )
                    }
                }
            }
            item(key = "caption") {
                Text(
                    text = readingCaption(content.mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.md),
                )
            }
        }
    }
}

/** How to read the heatmap, shown under the grid. */
private fun readingCaption(mode: RetentionMode): String {
    val unit = mode.periodNoun.lowercase()
    return "Each row is a group of visitors who first arrived on that date. The first column is their first visit " +
        "and is always 100%. Each later column shows the share of the group who came back that many ${unit}s later. " +
        "Size is the number of visitors in the group. Blank cells haven't happened yet."
}
