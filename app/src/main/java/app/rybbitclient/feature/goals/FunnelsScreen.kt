package app.rybbitclient.feature.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.model.Funnel
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.common.SubScreenScaffold
import app.rybbitclient.feature.common.toErrorCopy

private const val EMPTY_FUNNELS_TITLE = "No funnels yet"
private const val EMPTY_FUNNELS_MESSAGE =
    "Funnels are built in the Rybbit web dashboard. Saved funnels appear here with their steps."

/**
 * Saved funnels for the site. Tapping one opens its conversion analysis for the shared date range.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenFunnel]: Open FunnelDetail for the funnel id.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunnelsScreen(
    siteId: String,
    onOpenFunnel: (funnelId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: FunnelsViewModel = viewModel(key = "funnels-$siteId") {
        FunnelsViewModel(siteId, container.analyticsRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SubScreenScaffold(title = "Funnels", onBack = onBack) { padding ->
        when (val current = state) {
            FunnelsUiState.Loading -> LoadingView(modifier = Modifier.padding(padding).padding(Spacing.screenGutter))
            is FunnelsUiState.Error -> {
                val copy = current.error.toErrorCopy()
                ErrorView(
                    title = copy.title,
                    message = copy.message,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    onRetry = viewModel::refresh,
                )
            }
            is FunnelsUiState.Content -> FunnelsList(
                content = current,
                onRefresh = viewModel::refresh,
                onOpenFunnel = onOpenFunnel,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun FunnelsList(
    content: FunnelsUiState.Content,
    onRefresh: () -> Unit,
    onOpenFunnel: (Int) -> Unit,
    modifier: Modifier,
) {
    PullToRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(contentPadding = PaddingValues(vertical = Spacing.sm)) {
            if (content.funnels.isEmpty()) {
                item { EmptyView(title = EMPTY_FUNNELS_TITLE, message = EMPTY_FUNNELS_MESSAGE) }
            }
            items(content.funnels, key = { it.id }) { funnel ->
                FunnelRow(funnel = funnel, onClick = { onOpenFunnel(funnel.id) })
            }
        }
    }
}

@Composable
private fun FunnelRow(funnel: Funnel, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(funnel.name) },
        supportingContent = { Text(funnelSupportingText(funnel)) },
        leadingContent = { Icon(Icons.Outlined.FilterAlt, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun funnelSupportingText(funnel: Funnel): String {
    val count = funnel.steps.size
    val stepsLabel = if (count == 1) "1 step" else "$count steps"
    return "$stepsLabel · ${funnelStepPreview(funnel)}"
}
