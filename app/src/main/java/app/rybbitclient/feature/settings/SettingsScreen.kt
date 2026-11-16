package app.rybbitclient.feature.settings

import androidx.compose.runtime.Composable
import app.rybbitclient.feature.common.PlaceholderScreen

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [onBack]: Back navigation.
 * - [onAddInstance]: Open the add-instance flow.
 * - [onAllInstancesRemoved]: Called when the last instance is removed; navigate to onboarding.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun SettingsScreen(
    onBack: () -> Unit,
    onAddInstance: () -> Unit,
    onAllInstancesRemoved: () -> Unit,
) {
    PlaceholderScreen(title = "Settings", onBack = onBack)
}
