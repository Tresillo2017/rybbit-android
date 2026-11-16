package app.rybbitclient.feature.realtime

import androidx.compose.runtime.Composable
import app.rybbitclient.feature.common.PlaceholderScreen

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenSession]: Open SessionDetail.
 * - [onBack]: Back navigation.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun RealtimeScreen(
    siteId: String,
    onOpenSession: (sessionId: String) -> Unit,
    onBack: () -> Unit,
) {
    PlaceholderScreen(title = "Realtime", onBack = onBack)
}
