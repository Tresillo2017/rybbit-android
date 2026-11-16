package app.rybbitclient.feature.more

import app.rybbitclient.core.ui.components.hapticClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import app.rybbitclient.core.ui.components.ExpressiveRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.haptics.rememberHaptics
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.common.toErrorCopy

/**
 * The More tab: a list of secondary reports. This is the worked example for feature agents.
 * Shows the UiState pattern (Loading / Content / Error), pull-to-refresh, a ViewModel created with
 * LocalAppContainer, and conditional rows gated by feature availability.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - the on* callbacks open the report screens (routes defined in navigation/SiteTabHost.kt).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    siteId: String,
    onOpenGoals: () -> Unit,
    onOpenFunnels: () -> Unit,
    onOpenErrors: () -> Unit,
    onOpenRetention: () -> Unit,
    onOpenRealtime: () -> Unit,
    onOpenWebVitals: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: MoreViewModel = viewModel(key = "more-$siteId") {
        MoreViewModel(siteId, container.analyticsRepository, container.instanceRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val current = state) {
        MoreUiState.Loading -> LoadingView(modifier = Modifier.fillMaxSize())
        is MoreUiState.Error -> {
            val copy = current.error.toErrorCopy()
            ErrorView(
                title = copy.title,
                message = copy.message,
                modifier = Modifier.fillMaxSize(),
                onRetry = viewModel::refresh,
            )
        }
        is MoreUiState.Content -> {
            val links = moreLinks(
                content = current,
                onOpenGoals = onOpenGoals,
                onOpenFunnels = onOpenFunnels,
                onOpenErrors = onOpenErrors,
                onOpenRetention = onOpenRetention,
                onOpenRealtime = onOpenRealtime,
                onOpenWebVitals = onOpenWebVitals,
            )
            ExpressiveRefreshBox(
                isRefreshing = current.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(links, key = { _, link -> link.label }) { index, link ->
                        MoreRow(link = link, index = index, count = links.size)
                    }
                }
            }
        }
    }
}

/** One row in the list. [onClick] is invoked on tap; the whole row is the touch target. */
private data class MoreLink(
    val label: String,
    val supporting: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

private fun moreLinks(
    content: MoreUiState.Content,
    onOpenGoals: () -> Unit,
    onOpenFunnels: () -> Unit,
    onOpenErrors: () -> Unit,
    onOpenRetention: () -> Unit,
    onOpenRealtime: () -> Unit,
    onOpenWebVitals: () -> Unit,
): List<MoreLink> = buildList {
    add(MoreLink("Goals", "Conversions you track", Icons.Outlined.Flag, onOpenGoals))
    add(MoreLink("Funnels", funnelSummary(content.funnelCount), Icons.Outlined.FilterAlt, onOpenFunnels))
    add(MoreLink("Errors", "JavaScript errors grouped by message", Icons.Outlined.BugReport, onOpenErrors))
    add(MoreLink("Retention", "Returning visitors by cohort", Icons.Outlined.Repeat, onOpenRetention))
    add(MoreLink("Realtime", "Visitors in the last few minutes", Icons.Outlined.Timer, onOpenRealtime))
    if (content.webVitalsAvailable) {
        add(MoreLink("Web Vitals", "Page speed percentiles", Icons.Outlined.Speed, onOpenWebVitals))
    }
}

private fun funnelSummary(count: Int): String = when (count) {
    0 -> "No funnels yet"
    1 -> "1 funnel"
    else -> "$count funnels"
}

@Composable
private fun MoreRow(link: MoreLink, index: Int, count: Int) {
    val haptics = rememberHaptics()
    SegmentedListItem(
        onClick = {
            haptics.click()
            link.onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(),
        leadingContent = { Icon(link.icon, contentDescription = null) },
        supportingContent = { Text(link.supporting) },
        modifier = Modifier.padding(horizontal = Spacing.screenGutter),
    ) {
        Text(link.label)
    }
}
