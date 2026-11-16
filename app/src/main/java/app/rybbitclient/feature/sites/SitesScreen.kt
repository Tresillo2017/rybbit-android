package app.rybbitclient.feature.sites

import androidx.compose.runtime.Composable
import app.rybbitclient.feature.common.PlaceholderScreen

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [onSiteSelected]: siteId is Site.analyticsId; siteName is the display name.
 * - [onOpenSettings]: Open Settings (instance management).
 * - [onAddInstance]: Open the add-instance flow.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun SitesScreen(
    onSiteSelected: (siteId: String, siteName: String) -> Unit,
    onOpenSettings: () -> Unit,
    onAddInstance: () -> Unit,
) {
    PlaceholderScreen(title = "Sites", onBack = null)
}
