package app.rybbitclient.feature.realtime

import app.rybbitclient.core.ui.components.hapticClickable
import app.rybbitclient.core.ui.components.rememberCollapsingBarBehavior
import app.rybbitclient.core.ui.components.ExpressiveBackTopBar
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import app.rybbitclient.core.ui.components.ExpressiveRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.SectionCard
import app.rybbitclient.core.ui.components.Sparkline
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.countryFlagEmoji
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.format.formatRelativeTime
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.TabularNumbers
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.common.toErrorCopy

private const val LIVE_WINDOW_MINUTES = 5
private const val TREND_SKELETON_ROWS = 1
private const val RECENT_ACTIVITY_TITLE = "Recent activity"
private const val TREND_UNAVAILABLE = "The per-minute trend isn't available on this server right now."

/**
 * Realtime: the live visitor count, sessions per minute over the last 30 minutes, and a feed of the
 * newest pageviews and custom events. Polls only while the screen is visible.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenSession]: Open SessionDetail for a tapped feed row.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeScreen(
    siteId: String,
    onOpenSession: (sessionId: String) -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: RealtimeViewModel = viewModel(key = "realtime-$siteId") {
        RealtimeViewModel(siteId, container.analyticsRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.startPolling()
        onStopOrDispose { viewModel.stopPolling() }
    }
    val scrollBehavior = rememberCollapsingBarBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ExpressiveBackTopBar(
                title = "Realtime",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        RealtimeBody(
            state = state,
            onRefresh = viewModel::refresh,
            onOpenSession = onOpenSession,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun RealtimeBody(
    state: RealtimeUiState,
    onRefresh: () -> Unit,
    onOpenSession: (String) -> Unit,
    modifier: Modifier,
) {
    val error = state.error
    when {
        state.isInitialLoading -> LoadingView(modifier = modifier.fillMaxSize(), rowCount = 4)
        error != null && state.hasNoData -> {
            val copy = error.toErrorCopy()
            ErrorView(
                title = copy.title,
                message = copy.message,
                modifier = modifier.fillMaxSize(),
                onRetry = onRefresh,
            )
        }
        else -> ExpressiveRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            RealtimeList(state = state, onOpenSession = onOpenSession)
        }
    }
}

@Composable
private fun RealtimeList(state: RealtimeUiState, onOpenSession: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item(key = "count") { LiveCountCard(state.liveCount) }
        item(key = "trend") { TrendCard(state) }
        state.error?.let { error ->
            item(key = "error") { StaleBanner(error.toErrorCopy().message) }
        }
        item(key = "feed-header") {
            Text(
                text = RECENT_ACTIVITY_TITLE,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.screenGutter),
            )
        }
        val feed = state.feed.orEmpty()
        if (feed.isEmpty()) {
            item(key = "feed-empty") {
                EmptyView(
                    title = "No activity yet",
                    message = "Pageviews and custom events appear here as visitors arrive.",
                    modifier = Modifier.padding(horizontal = Spacing.screenGutter),
                )
            }
        } else {
            items(feed, key = { it.key }) { row -> FeedItem(row = row, onOpenSession = onOpenSession) }
        }
    }
}

@Composable
private fun LiveCountCard(count: Int?) {
    val countText = count?.let { formatFullNumber(it.toDouble()) } ?: EMPTY_VALUE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenGutter)
            .semantics { contentDescription = "$countText visitors in the last $LIVE_WINDOW_MINUTES minutes" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Live now", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = countText,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = TabularNumbers,
            ),
        )
        Text(
            text = "visitors in the last $LIVE_WINDOW_MINUTES minutes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrendCard(state: RealtimeUiState) {
    SectionCard(
        title = "Sessions per minute",
        subtitle = "Last 30 minutes",
        modifier = Modifier.padding(horizontal = Spacing.screenGutter),
    ) {
        val trend = state.trend
        when {
            trend != null && trend.isNotEmpty() -> Sparkline(values = trend)
            state.trendUnavailable -> Text(TREND_UNAVAILABLE, style = MaterialTheme.typography.bodyMedium)
            else -> LoadingView(modifier = Modifier.fillMaxWidth(), rowCount = TREND_SKELETON_ROWS)
        }
    }
}

@Composable
private fun StaleBanner(message: String) {
    SectionCard(title = "Couldn't refresh", modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
        Text(text = "$message Showing the last update.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FeedItem(row: FeedRow, onOpenSession: (String) -> Unit) {
    val sessionId = row.sessionId
    val flag = row.countryCode?.let { countryFlagEmoji(it) }.orEmpty()
    val supporting = listOfNotNull(
        if (row.isCustomEvent) "Custom event" else null,
        flag.takeIf { it.isNotEmpty() },
        row.browser,
        formatRelativeTime(row.timestampMillis),
    ).joinToString(separator = "  ·  ")
    ListItem(
        headlineContent = { Text(row.title, maxLines = 1) },
        supportingContent = { Text(supporting, maxLines = 1) },
        modifier = Modifier.then(
            if (sessionId != null) {
                Modifier.hapticClickable(role = Role.Button, onClick = { onOpenSession(sessionId) })
            } else {
                Modifier
            },
        ),
    )
}
