package app.rybbitclient.feature.onboarding

import androidx.compose.runtime.Composable
import app.rybbitclient.feature.common.PlaceholderScreen

/**
 * PLACEHOLDER: replace the body, keep this signature. Navigation depends on it.
 *
 * Parameters:
 * - [onAddCloud]: Start the Rybbit Cloud sign-in.
 * - [onAddSelfHosted]: Start the self-hosted sign-in.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun WelcomeScreen(
    onAddCloud: () -> Unit,
    onAddSelfHosted: () -> Unit,
) {
    PlaceholderScreen(title = "Welcome", onBack = null)
}
