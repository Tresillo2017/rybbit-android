package app.rybbitclient.feature.sessions

import app.rybbitclient.core.ui.components.rememberCollapsingBarBehavior
import app.rybbitclient.core.ui.components.ExpressiveBackTopBar
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import app.rybbitclient.core.ui.components.ExpressiveRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.UnavailableView
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.common.toErrorCopy
import java.time.ZoneId

private const val SCREEN_TITLE = "Session"
private const val BACK_DESCRIPTION = "Back"
private const val SHOW_PROPERTIES = "Show properties"
private const val HIDE_PROPERTIES = "Hide properties"
private const val NO_EVENTS_TITLE = "No events"
private const val NO_EVENTS_MESSAGE = "This session has no recorded events."
private val RAIL_WIDTH = 24.dp
private val RAIL_STROKE = 2.dp
private val DOT_RADIUS = 5.dp
private val DOT_TOP_OFFSET = 18.dp

/**
 * One session: a header card with the visitor, location, device and timing, then the timeline of its events
 * in local time, with the gap from the previous event. Tapping an event with properties expands its JSON.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [sessionId]: Session id from the sessions list.
 * - [onOpenUser]: Open UserDetail for the session's visitor.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    siteId: String,
    sessionId: String,
    onOpenUser: (userId: String) -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SessionDetailViewModel = viewModel(key = "session-$siteId-$sessionId") {
        SessionDetailViewModel(siteId, sessionId, container.analyticsRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = rememberCollapsingBarBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ExpressiveBackTopBar(
                title = SCREEN_TITLE,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        SessionDetailBody(
            state = state,
            zone = ZoneId.systemDefault(),
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onOpenUser = onOpenUser,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailBody(
    state: SessionDetailUiState,
    zone: ZoneId,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenUser: (userId: String) -> Unit,
    modifier: Modifier,
) {
    when (state) {
        SessionDetailUiState.Loading -> LoadingView(modifier = modifier)
        is SessionDetailUiState.Error -> {
            val copy = state.error.toErrorCopy()
            ErrorView(title = copy.title, message = copy.message, modifier = modifier, onRetry = onRefresh)
        }
        SessionDetailUiState.Unavailable -> UnavailableView(modifier = modifier)
        is SessionDetailUiState.Content -> ExpressiveRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
        ) {
            SessionDetailList(state, zone, onLoadMore, onOpenUser)
        }
    }
}

@Composable
private fun SessionDetailList(
    state: SessionDetailUiState.Content,
    zone: ZoneId,
    onLoadMore: () -> Unit,
    onOpenUser: (userId: String) -> Unit,
) {
    val listState = rememberLazyListState()
    val canLoadMore = state.hasMore && !state.isLoadingMore && state.loadMoreError == null
    listState.OnNearEnd(enabled = canLoadMore, onNearEnd = onLoadMore)
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = Spacing.xl)) {
        state.session?.let { session ->
            item(key = "header") {
                SessionHeaderCard(
                    session = session,
                    zone = zone,
                    onViewUser = onOpenUser,
                    modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = Spacing.sm),
                )
            }
        }
        if (state.timeline.isEmpty()) {
            item(key = "no-events") { EmptyView(title = NO_EVENTS_TITLE, message = NO_EVENTS_MESSAGE) }
        }
        items(items = state.timeline, key = { it.id }) { entry -> TimelineRow(entry) }
        if (state.isLoadingMore || state.loadMoreError != null) {
            item(key = "footer") { PagingFooter(state.loadMoreError, onLoadMore) }
        }
    }
}

/** One event: a rail with a dot, the local time and gap, the title, and optional expandable properties. */
@Composable
private fun TimelineRow(entry: TimelineEntry) {
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    val properties = entry.properties
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = Spacing.screenGutter),
    ) {
        TimelineRail(entry.kind)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm)
                .clickable(enabled = properties != null) { expanded = !expanded },
        ) {
            TimelineHeadline(entry)
            entry.subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (properties != null) {
                Text(
                    text = if (expanded) HIDE_PROPERTIES else SHOW_PROPERTIES,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.xxs),
                )
            }
            if (expanded && properties != null) {
                PropertiesBlock(properties)
            }
        }
    }
}

@Composable
private fun TimelineHeadline(entry: TimelineEntry) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = entry.timeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entry.sinceLabel?.let {
            Text(
                text = "  $it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Text(text = entry.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

/** Pretty JSON of the event properties. Long lines scroll sideways inside the block. */
@Composable
private fun PropertiesBlock(json: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
    ) {
        Text(
            text = json,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(Spacing.sm).horizontalScroll(rememberScrollState()),
        )
    }
}

/** The vertical line with a coloured dot for the event kind. */
@Composable
private fun TimelineRail(kind: TimelineKind) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = kindColor(kind)
    Box(
        modifier = Modifier
            .width(RAIL_WIDTH)
            .fillMaxHeight()
            .drawTimelineRail(lineColor, dotColor),
    )
}

@Composable
private fun kindColor(kind: TimelineKind): Color = when (kind) {
    TimelineKind.Pageview -> MaterialTheme.colorScheme.primary
    TimelineKind.CustomEvent -> MaterialTheme.colorScheme.tertiary
    TimelineKind.Other -> MaterialTheme.colorScheme.outline
}

private fun Modifier.drawTimelineRail(lineColor: Color, dotColor: Color): Modifier = drawBehind {
    val centerX = size.width / 2f
    drawLine(
        color = lineColor,
        start = Offset(centerX, 0f),
        end = Offset(centerX, size.height),
        strokeWidth = RAIL_STROKE.toPx(),
    )
    drawCircle(color = dotColor, radius = DOT_RADIUS.toPx(), center = Offset(centerX, DOT_TOP_OFFSET.toPx()))
}
