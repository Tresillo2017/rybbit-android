package app.rybbitclient.navigation

import androidx.navigation.NavController

/**
 * Named navigation actions. Screens receive plain lambdas and the navigation layer maps them here,
 * so feature code never touches a NavController.
 */
class AppNavigator(private val controller: NavController) {

    fun back() {
        controller.popBackStack()
    }

    /** Called once the startup route has read the instance list. */
    fun leaveStartup(hasInstances: Boolean) {
        if (hasInstances) {
            controller.navigate(SitesRoute) { popUpTo<SplashRoute> { inclusive = true } }
        } else {
            controller.navigate(OnboardingGraphRoute) { popUpTo<SplashRoute> { inclusive = true } }
        }
    }

    /** Replaces the main flow with onboarding. Used when the last instance is removed from Settings. */
    fun openOnboarding() {
        controller.navigate(OnboardingGraphRoute) { popUpTo<SitesRoute> { inclusive = true } }
    }

    /** First-run success: drops onboarding and lands on the site picker. */
    fun openSitesAfterSetup() {
        controller.navigate(SitesRoute) { popUpTo<OnboardingGraphRoute> { inclusive = true } }
    }

    /** Opens a site from the picker. Keeps the picker underneath so Back returns to it. */
    fun openSite(siteId: String, siteName: String) {
        controller.navigate(SiteRoute(siteId = siteId, siteName = siteName)) { popUpTo<SitesRoute>() }
    }

    /** Top-bar site switcher: replaces the current site shell with a fresh picker. */
    fun switchSite() {
        controller.navigate(SitesRoute) { popUpTo<SitesRoute> { inclusive = true } }
    }

    fun openSettings() {
        controller.navigate(SettingsRoute)
    }

    fun openAddInstance(isCloud: Boolean = false, fromOnboarding: Boolean = false) {
        controller.navigate(AddInstanceRoute(isCloud = isCloud, fromOnboarding = fromOnboarding))
    }

    fun openSessionDetail(siteId: String, sessionId: String) {
        controller.navigate(SessionDetailRoute(siteId = siteId, sessionId = sessionId))
    }

    fun openUserDetail(siteId: String, userId: String) {
        controller.navigate(UserDetailRoute(siteId = siteId, userId = userId))
    }

    fun openEventDetail(siteId: String, eventName: String) {
        controller.navigate(EventDetailRoute(siteId = siteId, eventName = eventName))
    }

    fun openErrorDetail(siteId: String, errorMessage: String) {
        controller.navigate(ErrorDetailRoute(siteId = siteId, errorMessage = errorMessage))
    }

    fun openGoals(siteId: String) {
        controller.navigate(GoalsRoute(siteId = siteId))
    }

    fun openFunnels(siteId: String) {
        controller.navigate(FunnelsRoute(siteId = siteId))
    }

    fun openFunnelDetail(siteId: String, funnelId: Int) {
        controller.navigate(FunnelDetailRoute(siteId = siteId, funnelId = funnelId))
    }

    fun openRetention(siteId: String) {
        controller.navigate(RetentionRoute(siteId = siteId))
    }

    fun openErrors(siteId: String) {
        controller.navigate(ErrorsRoute(siteId = siteId))
    }

    fun openRealtime(siteId: String) {
        controller.navigate(RealtimeRoute(siteId = siteId))
    }

    fun openWebVitals(siteId: String) {
        controller.navigate(WebVitalsRoute(siteId = siteId))
    }
}
