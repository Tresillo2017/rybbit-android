package app.rybbitclient.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.rybbitclient.feature.errors.ErrorDetailScreen
import app.rybbitclient.feature.errors.ErrorsScreen
import app.rybbitclient.feature.events.EventDetailScreen
import app.rybbitclient.feature.goals.FunnelDetailScreen
import app.rybbitclient.feature.goals.FunnelsScreen
import app.rybbitclient.feature.goals.GoalsScreen
import app.rybbitclient.feature.performance.WebVitalsScreen
import app.rybbitclient.feature.realtime.RealtimeScreen
import app.rybbitclient.feature.retention.RetentionScreen
import app.rybbitclient.feature.sessions.SessionDetailScreen
import app.rybbitclient.feature.users.UserDetailScreen

/** Full-screen detail and report routes. They sit above the site shell, so no tab bar is shown. */
fun NavGraphBuilder.detailRoutes(navigator: AppNavigator) {
    composable<SessionDetailRoute> { entry ->
        val route = entry.toRoute<SessionDetailRoute>()
        SessionDetailScreen(siteId = route.siteId, sessionId = route.sessionId, onBack = navigator::back)
    }
    composable<UserDetailRoute> { entry ->
        val route = entry.toRoute<UserDetailRoute>()
        UserDetailScreen(
            siteId = route.siteId,
            userId = route.userId,
            onOpenSession = { navigator.openSessionDetail(route.siteId, it) },
            onBack = navigator::back,
        )
    }
    composable<EventDetailRoute> { entry ->
        val route = entry.toRoute<EventDetailRoute>()
        EventDetailScreen(siteId = route.siteId, eventName = route.eventName, onBack = navigator::back)
    }
    composable<ErrorDetailRoute> { entry ->
        val route = entry.toRoute<ErrorDetailRoute>()
        ErrorDetailScreen(siteId = route.siteId, errorMessage = route.errorMessage, onBack = navigator::back)
    }
    composable<GoalsRoute> { entry ->
        GoalsScreen(siteId = entry.toRoute<GoalsRoute>().siteId, onBack = navigator::back)
    }
    composable<FunnelsRoute> { entry ->
        val siteId = entry.toRoute<FunnelsRoute>().siteId
        FunnelsScreen(
            siteId = siteId,
            onOpenFunnel = { navigator.openFunnelDetail(siteId, it) },
            onBack = navigator::back,
        )
    }
    composable<FunnelDetailRoute> { entry ->
        val route = entry.toRoute<FunnelDetailRoute>()
        FunnelDetailScreen(siteId = route.siteId, funnelId = route.funnelId, onBack = navigator::back)
    }
    composable<RetentionRoute> { entry ->
        RetentionScreen(siteId = entry.toRoute<RetentionRoute>().siteId, onBack = navigator::back)
    }
    composable<ErrorsRoute> { entry ->
        val siteId = entry.toRoute<ErrorsRoute>().siteId
        ErrorsScreen(
            siteId = siteId,
            onOpenError = { navigator.openErrorDetail(siteId, it) },
            onBack = navigator::back,
        )
    }
    composable<RealtimeRoute> { entry ->
        val siteId = entry.toRoute<RealtimeRoute>().siteId
        RealtimeScreen(
            siteId = siteId,
            onOpenSession = { navigator.openSessionDetail(siteId, it) },
            onBack = navigator::back,
        )
    }
    composable<WebVitalsRoute> { entry ->
        WebVitalsScreen(siteId = entry.toRoute<WebVitalsRoute>().siteId, onBack = navigator::back)
    }
}
