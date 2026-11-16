package app.rybbitclient.feature.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.model.Goal
import app.rybbitclient.core.model.SessionSummary
import app.rybbitclient.core.network.RybbitTimestamps
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.countryLabel
import app.rybbitclient.core.ui.format.formatDuration
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.format.formatPercent
import app.rybbitclient.core.ui.format.formatRelativeTime
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.rybbitColors
import app.rybbitclient.feature.goals.shared.ApiErrorContent

private const val GOAL_TARGET_MAX_LINES = 1
private const val PERCENT_SCALE = 100.0
private const val SESSION_SHEET_MAX_HEIGHT_DP = 420
private const val UNKNOWN_LOCATION = "Unknown location"

/** One goal: name, Path/Event badge, the tracked pattern or event, conversion bar and counts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCard(goal: Goal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val fraction = goalConversionFraction(goal)
    val conversions = goal.totalConversions ?: 0
    val sessions = goal.totalSessions ?: 0
    val rateText = fraction?.let { formatPercent(it * PERCENT_SCALE) } ?: EMPTY_VALUE
    val summary = "${goal.name}, ${goalKindOf(goal.goalType).badge}, $rateText conversion, " +
        "${formatFullNumber(conversions.toDouble())} of ${formatFullNumber(sessions.toDouble())} sessions"
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = summary },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.sm))
                GoalKindBadge(goalKindOf(goal.goalType))
            }
            goalTargetOf(goal)?.let { target ->
                Text(
                    text = target,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = GOAL_TARGET_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LinearProgressIndicator(
                progress = { (fraction ?: 0.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.rybbitColors.positive,
                trackColor = MaterialTheme.rybbitColors.barTrack,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${formatFullNumber(conversions.toDouble())} of ${formatFullNumber(sessions.toDouble())} sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = rateText, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun GoalKindBadge(kind: GoalKind) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = kind.badge,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        )
    }
}

/**
 * Sessions that converted [state]'s goal, in a modal bottom sheet. Tapping a session calls [onOpenSession];
 * a failed load calls [onRetry] with the goal. Renders nothing while [state] is [GoalSessionsState.Closed].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSessionsSheet(
    state: GoalSessionsState,
    onDismiss: () -> Unit,
    onRetry: (Goal) -> Unit,
    onOpenSession: (sessionId: String) -> Unit,
) {
    if (state == GoalSessionsState.Closed) return
    val goal = when (state) {
        is GoalSessionsState.Loading -> state.goal
        is GoalSessionsState.Loaded -> state.goal
        is GoalSessionsState.Failed -> state.goal
        GoalSessionsState.Closed -> return
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(horizontal = Spacing.screenGutter).padding(bottom = Spacing.xl)) {
            Text(text = goal.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.md))
            when (state) {
                is GoalSessionsState.Loading -> LoadingView(rowCount = 3)
                is GoalSessionsState.Failed -> ApiErrorContent(
                    error = state.error,
                    onRetry = { onRetry(goal) },
                )
                is GoalSessionsState.Loaded -> SessionsList(state.sessions, onOpenSession)
                GoalSessionsState.Closed -> Unit
            }
        }
    }
}

@Composable
private fun SessionsList(sessions: List<SessionSummary>, onOpenSession: (String) -> Unit) {
    if (sessions.isEmpty()) {
        EmptyView(
            title = "No sessions yet",
            message = "No visitor converted this goal in the selected period.",
        )
        return
    }
    LazyColumn(modifier = Modifier.heightIn(max = SESSION_SHEET_MAX_HEIGHT_DP.dp)) {
        items(sessions, key = { it.sessionId }) { session ->
            ListItem(
                headlineContent = { Text(sessionLocation(session)) },
                supportingContent = { Text(sessionSupportingText(session)) },
                modifier = Modifier.clickable { onOpenSession(session.sessionId) },
            )
        }
    }
}

private fun sessionLocation(session: SessionSummary): String =
    session.country?.takeIf { it.isNotBlank() }?.let { countryLabel(it) } ?: UNKNOWN_LOCATION

private fun sessionSupportingText(session: SessionSummary): String {
    val started = RybbitTimestamps.parseUtcOrNull(session.sessionStart)
        ?.let { formatRelativeTime(it.toEpochMilli()) }
        ?: EMPTY_VALUE
    val duration = session.sessionDuration?.let { formatDuration(it) } ?: EMPTY_VALUE
    return "$started · $duration"
}
