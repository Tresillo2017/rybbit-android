@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.rybbitclient.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.core.ui.theme.Spacing

private const val SKELETON_ROW_HEIGHT_DP = 56
private const val SKELETON_CORNER_DP = 12
private const val ICON_SIZE_DP = 48
private const val RETRY_ICON_SIZE_DP = 18
private const val MESSAGE_MAX_WIDTH_DP = 360
private const val LOADING_DESCRIPTION = "Loading"

/** Loading state. Either animated skeleton rows (default) or a centered spinner. */
@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
    skeleton: Boolean = true,
    rowCount: Int = 3,
) {
    if (skeleton) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
                .semantics { contentDescription = LOADING_DESCRIPTION },
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(rowCount.coerceAtLeast(1)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SKELETON_ROW_HEIGHT_DP.dp)
                        .shimmerPlaceholder(RoundedCornerShape(SKELETON_CORNER_DP.dp)),
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(Spacing.xl)
                .semantics { contentDescription = LOADING_DESCRIPTION },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

/** Failure state with an optional retry action. Copy should already be user-friendly. */
@Composable
fun ErrorView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Try again",
) {
    StateMessage(
        icon = Icons.Outlined.ErrorOutline,
        iconTint = MaterialTheme.colorScheme.error,
        title = title,
        message = message,
        modifier = modifier,
        assertiveLiveRegion = true,
        onRetry = onRetry,
        retryLabel = retryLabel,
    )
}

/** Nothing to show yet, e.g. a filter matched no rows. */
@Composable
fun EmptyView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
) {
    StateMessage(
        icon = icon,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        title = title,
        message = message,
        modifier = modifier,
    )
}

/** Feature exists in Rybbit Cloud but not on this self-hosted build. Calm, never red. */
@Composable
fun UnavailableView(
    modifier: Modifier = Modifier,
    title: String = "Not available on this server",
    message: String = "This report needs a cloud-only or newer Rybbit feature. The rest of the dashboard works normally.",
) {
    StateMessage(
        icon = Icons.Outlined.CloudOff,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        title = title,
        message = message,
        modifier = modifier,
    )
}

@Composable
private fun StateMessage(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    modifier: Modifier,
    assertiveLiveRegion: Boolean = false,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenGutter, vertical = Spacing.xl)
            .semantics {
                if (assertiveLiveRegion) liveRegion = LiveRegionMode.Assertive
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(ICON_SIZE_DP.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = MESSAGE_MAX_WIDTH_DP.dp),
        )
        if (onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .padding(top = Spacing.xs)
                    .minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(RETRY_ICON_SIZE_DP.dp),
                )
                Text(text = retryLabel, modifier = Modifier.padding(start = Spacing.xs))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingSkeletonPreview() {
    RybbitTheme { LoadingView() }
}

@Preview(showBackground = true)
@Composable
private fun ErrorViewPreview() {
    RybbitTheme {
        ErrorView(
            title = "Couldn't load overview",
            message = "Check your connection and try again.",
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyViewPreview() {
    RybbitTheme {
        EmptyView(title = "No sessions yet", message = "Sessions appear here once visitors arrive.")
    }
}

@Preview(showBackground = true)
@Composable
private fun UnavailableViewPreview() {
    RybbitTheme { UnavailableView() }
}
