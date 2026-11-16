package app.rybbitclient.feature.users

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import app.rybbitclient.feature.common.PlaceholderBody

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenUser]: Open UserDetail.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun UsersScreen(
    siteId: String,
    onOpenUser: (userId: String) -> Unit,
) {
    PlaceholderBody(name = "Users", modifier = Modifier.fillMaxSize())
}
