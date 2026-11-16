package app.rybbitclient.feature.sessions

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

private const val PREFETCH_ITEMS = 5

/**
 * Calls [onNearEnd] when the last visible item is within [PREFETCH_ITEMS] of the end of the list and [enabled].
 * Put it in a list that renders its loading footer as the last item, so the footer itself counts as the end.
 */
@Composable
fun LazyListState.OnNearEnd(enabled: Boolean, onNearEnd: () -> Unit) {
    val isNearEnd by remember(this) {
        derivedStateOf {
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisible >= layoutInfo.totalItemsCount - 1 - PREFETCH_ITEMS
        }
    }
    LaunchedEffect(isNearEnd, enabled) {
        if (enabled && isNearEnd) onNearEnd()
    }
}
