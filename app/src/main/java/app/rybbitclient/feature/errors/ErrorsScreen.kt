package app.rybbitclient.feature.errors

import androidx.compose.runtime.Composable
import app.rybbitclient.feature.common.PlaceholderScreen

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenError]: Open ErrorDetail for an error message.
 * - [onBack]: Back navigation.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun ErrorsScreen(
    siteId: String,
    onOpenError: (errorMessage: String) -> Unit,
    onBack: () -> Unit,
) {
    PlaceholderScreen(title = "Errors", onBack = onBack)
}
