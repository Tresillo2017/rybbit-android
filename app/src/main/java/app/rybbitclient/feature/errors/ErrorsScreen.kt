package app.rybbitclient.feature.errors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.model.ErrorName
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.UnavailableView
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.format.formatPercent
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.events.ApiErrorView
import app.rybbitclient.feature.events.DetailScaffold
import app.rybbitclient.feature.events.SiteQueryControls

private const val PERCENT_SCALE = 100.0
private const val EMPTY_TITLE = "No errors in this period"
private const val EMPTY_MESSAGE = "No JavaScript errors were recorded for the selected range."

/**
 * Errors report (reached from More): distinct error messages with occurrences, affected sessions and
 * their share of all errors. Tapping a message opens ErrorDetail.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenError]: Open ErrorDetail for an error message, exactly as grouped by the server.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorsScreen(
    siteId: String,
    onOpenError: (errorMessage: String) -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ErrorsViewModel = viewModel(key = "errors-$siteId") {
        ErrorsViewModel(siteId, container.analyticsRepository, container.siteQueryState)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by container.siteQueryState.query.collectAsStateWithLifecycle()

    DetailScaffold(title = "Errors", onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SiteQueryControls(
                queryState = container.siteQueryState,
                query = query,
                modifier = Modifier.padding(vertical = Spacing.sm),
            )
            when (val current = state) {
                ErrorsUiState.Loading -> LoadingView(modifier = Modifier.fillMaxSize())
                is ErrorsUiState.Error -> ApiErrorView(
                    error = current.error,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                )
                ErrorsUiState.Unavailable -> UnavailableView(modifier = Modifier.fillMaxSize())
                is ErrorsUiState.Content -> ErrorsList(
                    content = current,
                    onRefresh = viewModel::refresh,
                    onOpenError = onOpenError,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorsList(
    content: ErrorsUiState.Content,
    onRefresh: () -> Unit,
    onOpenError: (String) -> Unit,
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
            if (content.errors.isEmpty()) {
                item { EmptyView(title = EMPTY_TITLE, message = EMPTY_MESSAGE) }
            }
            itemsIndexed(content.errors, key = { index, error -> "$index:${error.value}" }) { _, error ->
                ErrorNameCard(error = error, onClick = { onOpenError(error.value) })
            }
        }
    }
}

@Composable
private fun ErrorNameCard(error: ErrorName, onClick: () -> Unit) {
    val count = error.count ?: 0
    val sessions = error.sessionCount ?: 0
    val share = error.percentage?.let { formatPercent(it) } ?: EMPTY_VALUE
    val summary = listOfNotNull(
        error.value,
        error.errorName,
        "${formatFullNumber(count.toDouble())} occurrences",
        "${formatFullNumber(sessions.toDouble())} sessions",
        "$share of errors",
    ).joinToString(separator = ". ")

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = summary },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = error.value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            error.errorName?.takeIf { it.isNotBlank() }?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { ((error.percentage ?: 0.0) / PERCENT_SCALE).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                ErrorStat(label = "Occurrences", value = formatFullNumber(count.toDouble()))
                ErrorStat(label = "Sessions", value = formatFullNumber(sessions.toDouble()))
                ErrorStat(label = "Share", value = share)
            }
        }
    }
}

@Composable
private fun ErrorStat(label: String, value: String) {
    Column {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
