package app.rybbitclient.feature.goals.shared

import androidx.compose.ui.Modifier
import app.rybbitclient.core.ui.components.rememberCollapsingBarBehavior
import app.rybbitclient.core.ui.components.ExpressiveBackTopBar
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

/**
 * Chrome for a report opened from the More tab: a top bar with a back arrow, then [content] with the
 * scaffold's padding applied by the caller. Shared by the goals, funnel, retention and vitals screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = rememberCollapsingBarBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ExpressiveBackTopBar(
                title = title,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
        content = content,
    )
}
