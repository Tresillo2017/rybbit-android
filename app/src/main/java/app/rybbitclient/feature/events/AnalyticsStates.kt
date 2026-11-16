package app.rybbitclient.feature.events

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.feature.common.toErrorCopy

/** Error state for a failed analytics load: friendly copy from [toErrorCopy] and a retry action. */
@Composable
fun ApiErrorView(
    error: ApiException,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = error.toErrorCopy()
    ErrorView(title = copy.title, message = copy.message, modifier = modifier, onRetry = onRetry)
}
