package app.rybbitclient.feature.errors

import androidx.compose.runtime.Composable
import app.rybbitclient.feature.common.PlaceholderScreen

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [errorMessage]: Error message, exactly as grouped by the server.
 * - [onBack]: Back navigation.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun ErrorDetailScreen(
    siteId: String,
    errorMessage: String,
    onBack: () -> Unit,
) {
    PlaceholderScreen(title = "Error", onBack = onBack)
}
