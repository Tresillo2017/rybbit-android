package app.rybbitclient.feature.sessions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import app.rybbitclient.feature.common.PlaceholderBody

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenSession]: Open SessionDetail.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun SessionsScreen(
    siteId: String,
    onOpenSession: (sessionId: String) -> Unit,
) {
    PlaceholderBody(name = "Sessions", modifier = Modifier.fillMaxSize())
}
