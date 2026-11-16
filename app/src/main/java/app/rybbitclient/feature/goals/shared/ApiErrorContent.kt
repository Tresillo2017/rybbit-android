package app.rybbitclient.feature.goals.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.UnavailableView
import app.rybbitclient.feature.common.toErrorCopy

/**
 * Renders a failed request. A [FeatureUnavailable] (the route is missing on this Rybbit build) shows the
 * calm unavailable state with no retry; anything else shows a friendly error with [onRetry].
 */
@Composable
fun ApiErrorContent(
    error: ApiException,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val copy = error.toErrorCopy()
    if (error is FeatureUnavailable) {
        UnavailableView(modifier = modifier, title = copy.title, message = copy.message)
    } else {
        ErrorView(title = copy.title, message = copy.message, modifier = modifier, onRetry = onRetry)
    }
}
