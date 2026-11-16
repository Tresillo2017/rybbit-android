package app.rybbitclient.feature.onboarding

import androidx.compose.runtime.Composable
import app.rybbitclient.feature.common.PlaceholderScreen

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [isCloud]: True when the Cloud preset was chosen; false for self-hosted.
 * - [onInstanceAdded]: Called after the instance and credential are saved.
 * - [onBack]: Back navigation.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun AddInstanceScreen(
    isCloud: Boolean,
    onInstanceAdded: () -> Unit,
    onBack: () -> Unit,
) {
    PlaceholderScreen(title = "Add instance", onBack = onBack)
}
