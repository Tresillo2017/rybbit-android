package app.rybbitclient.feature.users

import androidx.compose.runtime.Composable
import app.rybbitclient.feature.common.PlaceholderScreen

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [userId]: User id from the users list.
 * - [onOpenSession]: Open SessionDetail.
 * - [onBack]: Back navigation.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun UserDetailScreen(
    siteId: String,
    userId: String,
    onOpenSession: (sessionId: String) -> Unit,
    onBack: () -> Unit,
) {
    PlaceholderScreen(title = "User", onBack = onBack)
}
