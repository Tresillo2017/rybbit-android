package app.rybbitclient.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.rybbitclient.feature.onboarding.AddInstanceScreen
import app.rybbitclient.feature.settings.SettingsScreen
import app.rybbitclient.feature.sites.SitesScreen

/** Top-level screens of the main flow: the site picker, the shell of each site, and Settings. */
fun NavGraphBuilder.rootRoutes(navigator: AppNavigator) {
    composable<AddInstanceRoute> { entry ->
        val route = entry.toRoute<AddInstanceRoute>()
        AddInstanceScreen(
            isCloud = route.isCloud,
            onInstanceAdded = if (route.fromOnboarding) navigator::openSitesAfterSetup else navigator::back,
            onBack = navigator::back,
        )
    }
    composable<SitesRoute> {
        SitesScreen(
            onSiteSelected = navigator::openSite,
            onOpenSettings = navigator::openSettings,
            onAddInstance = { navigator.openAddInstance() },
        )
    }
    composable<SiteRoute> { entry ->
        val route = entry.toRoute<SiteRoute>()
        SiteScaffold(siteId = route.siteId, siteName = route.siteName, navigator = navigator)
    }
    composable<SettingsRoute> {
        SettingsScreen(
            onBack = navigator::back,
            onAddInstance = { navigator.openAddInstance() },
            onAllInstancesRemoved = navigator::openOnboarding,
        )
    }
}
