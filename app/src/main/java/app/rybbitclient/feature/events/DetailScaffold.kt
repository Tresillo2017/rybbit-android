package app.rybbitclient.feature.events

import androidx.compose.ui.Modifier
import app.rybbitclient.core.ui.components.rememberCollapsingBarBehavior
import app.rybbitclient.core.ui.components.ExpressiveBackTopBar
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

private const val BACK_DESCRIPTION = "Back"

/** Chrome for full-screen analytics details: a top bar with a back arrow, then the body. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScaffold(
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
