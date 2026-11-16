package app.rybbitclient.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.rybbitclient.feature.events.EventsScreen
import app.rybbitclient.feature.more.MoreScreen
import app.rybbitclient.feature.overview.OverviewScreen
import app.rybbitclient.feature.sessions.SessionsScreen
import app.rybbitclient.feature.users.UsersScreen

/** The inner NavHost: maps each tab route to its feature screen, passing the site id and outer navigation actions. */
@Composable
fun SiteTabHost(
    tabController: NavHostController,
    siteId: String,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
) {
    val (fadeIn, fadeOut) = rememberTabFade()
    NavHost(
        navController = tabController,
        startDestination = OverviewTabRoute,
        modifier = modifier,
        enterTransition = fadeIn,
        exitTransition = fadeOut,
        popEnterTransition = fadeIn,
        popExitTransition = fadeOut,
    ) {
        composable<OverviewTabRoute> {
            OverviewScreen(siteId = siteId, onOpenRealtime = { navigator.openRealtime(siteId) })
        }
        composable<SessionsTabRoute> {
            SessionsScreen(siteId = siteId, onOpenSession = { navigator.openSessionDetail(siteId, it) })
        }
        composable<EventsTabRoute> {
            EventsScreen(siteId = siteId, onOpenEvent = { navigator.openEventDetail(siteId, it) })
        }
        composable<UsersTabRoute> {
            UsersScreen(siteId = siteId, onOpenUser = { navigator.openUserDetail(siteId, it) })
        }
        composable<MoreTabRoute> {
            MoreScreen(
                siteId = siteId,
                onOpenGoals = { navigator.openGoals(siteId) },
                onOpenFunnels = { navigator.openFunnels(siteId) },
                onOpenErrors = { navigator.openErrors(siteId) },
                onOpenRetention = { navigator.openRetention(siteId) },
                onOpenRealtime = { navigator.openRealtime(siteId) },
                onOpenWebVitals = { navigator.openWebVitals(siteId) },
            )
        }
    }
}
