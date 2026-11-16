package app.rybbitclient.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import app.rybbitclient.core.ui.haptics.rememberHaptics

/** Scroll behaviour shared by every collapsing bar: large title that folds away as content scrolls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberCollapsingBarBehavior(): TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

/**
 * Collapsing Expressive top bar for pushed screens: a large title and a back arrow with a light haptic.
 * Pair [scrollBehavior] with `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` on the Scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveBackTopBar(
    title: String,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val haptics = rememberHaptics()
    MediumFlexibleTopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                haptics.click()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}
