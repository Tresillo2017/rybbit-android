package app.rybbitclient.feature.goals

import androidx.compose.runtime.Composable
import app.rybbitclient.feature.common.PlaceholderScreen

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenFunnel]: Open FunnelDetail.
 * - [onBack]: Back navigation.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun FunnelsScreen(
    siteId: String,
    onOpenFunnel: (funnelId: Int) -> Unit,
    onBack: () -> Unit,
) {
    PlaceholderScreen(title = "Funnels", onBack = onBack)
}
