package app.rybbitclient.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.rybbitclient.core.ui.haptics.Haptics
import app.rybbitclient.core.ui.haptics.rememberHaptics
import app.rybbitclient.di.LocalAppContainer

/** Windows at least this wide (dp) swap the bottom bar for a side rail (Material "medium" window class). */
private const val RAIL_MIN_WIDTH_DP = 600

/**
 * Chrome for one site: a collapsing top bar (name, site switcher, overflow to Settings), adaptive
 * navigation, and an inner NavHost that holds the tab screens. Phones get the Expressive short navigation
 * bar; windows 600dp and wider (tablets, unfolded foldables, desktop mode) get a rail. The top bar collapses
 * as any tab scrolls, because the scroll behavior's connection sits on the Scaffold and child lists
 * dispatch into it. Switching tabs gives a selection haptic.
 * Entering a site notifies [app.rybbitclient.core.query.SiteQueryState] so filters reset on a site change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteScaffold(siteId: String, siteName: String, navigator: AppNavigator) {
    val siteQueryState = LocalAppContainer.current.siteQueryState
    LaunchedEffect(siteId) { siteQueryState.onSiteOpened(siteId) }
    val tabController = rememberNavController()
    val currentDestination = tabController.currentBackStackEntryAsState().value?.destination
    val haptics = rememberHaptics()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val useRail = LocalConfiguration.current.screenWidthDp >= RAIL_MIN_WIDTH_DP

    val isSelected: (SiteTab) -> Boolean = { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
    }
    val onTabClick: (SiteTab) -> Unit = { tab ->
        if (!isSelected(tab)) haptics.selection()
        tabController.selectSiteTab(tab)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SiteTopBar(
                siteName = siteName,
                scrollBehavior = scrollBehavior,
                haptics = haptics,
                onSwitchSite = navigator::switchSite,
                onOpenSettings = navigator::openSettings,
            )
        },
        bottomBar = {
            if (!useRail) {
                ShortNavigationBar {
                    SiteTab.entries.forEach { tab ->
                        ShortNavigationBarItem(
                            selected = isSelected(tab),
                            onClick = { onTabClick(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (useRail) {
                NavigationRail {
                    SiteTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = isSelected(tab),
                            onClick = { onTabClick(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
            SiteTabHost(
                tabController = tabController,
                siteId = siteId,
                navigator = navigator,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Medium flexible top bar: the site name sits large under the actions and collapses to a single line
 * as the content scrolls. The overflow menu leads to Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiteTopBar(
    siteName: String,
    scrollBehavior: TopAppBarScrollBehavior,
    haptics: Haptics,
    onSwitchSite: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = {
            Text(
                text = siteName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        actions = {
            IconButton(onClick = {
                haptics.click()
                onSwitchSite()
            }) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = "Switch site")
            }
            IconButton(onClick = {
                haptics.click()
                menuOpen = true
            }) {
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
        scrollBehavior = scrollBehavior,
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
