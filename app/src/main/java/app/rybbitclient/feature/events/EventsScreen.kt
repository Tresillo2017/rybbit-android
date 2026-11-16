package app.rybbitclient.feature.events

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import app.rybbitclient.feature.common.PlaceholderBody

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenEvent]: Open EventDetail for an event name.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun EventsScreen(
    siteId: String,
    onOpenEvent: (eventName: String) -> Unit,
) {
    PlaceholderBody(name = "Events", modifier = Modifier.fillMaxSize())
}
