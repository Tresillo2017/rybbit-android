package app.rybbitclient.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.rybbitclient.di.LocalAppContainer

/**
 * Chrome for one site: a top bar (name, site switcher, overflow to Settings), the five-tab bottom bar,
 * and an inner NavHost that holds the tab screens. The tab state lives in [SiteTabHost].
 * Entering a site notifies [app.rybbitclient.core.query.SiteQueryState] so filters reset on a site change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteScaffold(siteId: String, siteName: String, navigator: AppNavigator) {
    val siteQueryState = LocalAppContainer.current.siteQueryState
    LaunchedEffect(siteId) { siteQueryState.onSiteOpened(siteId) }
    val tabController = rememberNavController()
    val currentDestination = tabController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        topBar = {
            SiteTopBar(
                siteName = siteName,
                onSwitchSite = navigator::switchSite,
                onOpenSettings = navigator::openSettings,
            )
        },
        bottomBar = {
            NavigationBar {
                SiteTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true,
                        onClick = { tabController.selectSiteTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        SiteTabHost(
            tabController = tabController,
            siteId = siteId,
            navigator = navigator,
            modifier = Modifier.padding(padding),
        )
    }
}

/** Top bar with the site name, a site-switch action and an overflow menu that leads to Settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiteTopBar(
    siteName: String,
    onSwitchSite: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(text = siteName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        actions = {
            IconButton(onClick = onSwitchSite) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = "Switch site")
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        menuOpen = false
                        onOpenSettings()
                    },
                )
            }
        },
    )
}

/** Switches the inner tab NavHost. Keeps one copy of each tab, so tab state survives switching. */
internal fun NavHostController.selectSiteTab(tab: SiteTab) {
    navigate(tab.route) {
        popUpTo<OverviewTabRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
