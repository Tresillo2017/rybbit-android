package app.rybbitclient.feature.overview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import app.rybbitclient.feature.common.PlaceholderBody

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenRealtime]: Open the Realtime screen (from the live badge).
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun OverviewScreen(
    siteId: String,
    onOpenRealtime: () -> Unit,
) {
    PlaceholderBody(name = "Overview", modifier = Modifier.fillMaxSize())
}
