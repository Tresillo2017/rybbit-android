package app.rybbitclient.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import app.rybbitclient.core.ui.components.ExpressiveRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.model.Goal
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.common.toErrorCopy
import app.rybbitclient.feature.goals.shared.ApiErrorContent
import app.rybbitclient.feature.goals.shared.SiteQueryControls
import app.rybbitclient.feature.goals.shared.SubScreenScaffold

private const val LOAD_MORE_PREFETCH_ITEMS = 3
private const val EMPTY_GOALS_TITLE = "No goals yet"
private const val EMPTY_GOALS_MESSAGE =
    "Goals are created in the Rybbit web dashboard. Once one is set up, its conversions appear here."

/**
 * Goals with conversion rate bars, paged as the list is scrolled. Tapping a goal opens the sessions
 * that converted it in a bottom sheet; tapping a session calls [onOpenSession].
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenSession]: Open SessionDetail for the given session id.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    siteId: String,
    onOpenSession: (sessionId: String) -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: GoalsViewModel = viewModel(key = "goals-$siteId") {
        GoalsViewModel(siteId, container.analyticsRepository, container.siteQueryState)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheet by viewModel.sheetState.collectAsStateWithLifecycle()
    val query by container.siteQueryState.query.collectAsStateWithLifecycle()

    SubScreenScaffold(title = "Goals", onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SiteQueryControls(query = query, siteQuery = container.siteQueryState)
            GoalsBody(
                state = state,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadNextPage,
                onOpenGoal = viewModel::openGoalSessions,
            )
        }
    }
    GoalSessionsSheet(
        state = sheet,
        onDismiss = viewModel::closeGoalSessions,
        onRetry = viewModel::openGoalSessions,
        onOpenSession = { sessionId ->
            viewModel.closeGoalSessions()
            onOpenSession(sessionId)
        },
    )
}

@Composable
private fun GoalsBody(
    state: GoalsUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenGoal: (Goal) -> Unit,
) {
    when (state) {
        GoalsUiState.Loading -> LoadingView(modifier = Modifier.fillMaxWidth().padding(Spacing.screenGutter))
        is GoalsUiState.Error -> ApiErrorContent(
            error = state.error,
            modifier = Modifier.fillMaxSize(),
            onRetry = onRefresh,
        )
        is GoalsUiState.Content -> GoalsList(state, onRefresh, onLoadMore, onOpenGoal)
    }
}

@Composable
private fun GoalsList(
    content: GoalsUiState.Content,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenGoal: (Goal) -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd = nearEndOfList(listState, content.goals.size)
    LaunchedEffect(nearEnd, content.hasMore, content.isLoadingMore, content.loadMoreError) {
        if (nearEnd && content.hasMore && !content.isLoadingMore && content.loadMoreError == null) onLoadMore()
    }
    ExpressiveRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = Spacing.xl)) {
            if (content.goals.isEmpty()) {
                item { EmptyView(title = EMPTY_GOALS_TITLE, message = EMPTY_GOALS_MESSAGE) }
            }
            items(content.goals, key = { it.goalId }) { goal ->
                GoalCard(
                    goal = goal,
                    onClick = { onOpenGoal(goal) },
                    modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = Spacing.xs),
                )
            }
            item(key = "footer") {
                LoadMoreFooter(content = content, onLoadMore = onLoadMore)
            }
        }
    }
}

/** True once the last visible row is within [LOAD_MORE_PREFETCH_ITEMS] of the final goal. */
@Composable
private fun nearEndOfList(listState: LazyListState, goalCount: Int): Boolean {
    val nearEnd by remember(goalCount) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= goalCount - LOAD_MORE_PREFETCH_ITEMS
        }
    }
    return nearEnd
}

@Composable
private fun LoadMoreFooter(content: GoalsUiState.Content, onLoadMore: () -> Unit) {
    val error = content.loadMoreError
    when {
        content.isLoadingMore -> Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) { LoadingIndicator() }
        error != null -> Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = error.toErrorCopy().message)
            TextButton(onClick = onLoadMore) { Text("Try again") }
        }
        else -> Unit
    }
}
