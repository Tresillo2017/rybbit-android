package app.rybbitclient.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe navigation routes. Every route class ends in `Route` so it never collides with the
 * domain models in core.model (Site, Goal, Funnel, Event, UserDetail...).
 * Routes carry plain ids only (siteId is Site.analyticsId, the URL id). Never pass model objects.
 */

/** Startup: reads the instance list, then replaces itself with onboarding or the site picker. */
@Serializable
data object SplashRoute

/** Nested graph for first-run setup. Start destination: [WelcomeRoute]. */
@Serializable
data object OnboardingGraphRoute

@Serializable
data object WelcomeRoute

/**
 * Adds an instance. Lives on the root graph, not inside onboarding, so opening it from Settings does not
 * push the onboarding start screen onto the back stack. [isCloud] pre-selects the Cloud preset.
 * [fromOnboarding] decides where success goes: the site picker (first run) or back to the caller.
 */
@Serializable
data class AddInstanceRoute(val isCloud: Boolean = false, val fromOnboarding: Boolean = false)

/** Site picker for the active instance. Root of the main flow. */
@Serializable
data object SitesRoute

/** Instance management (list, rename, remove, re-login). */
@Serializable
data object SettingsRoute

/**
 * One site's shell: top bar, bottom tabs and an inner NavHost of [OverviewTabRoute]..[MoreTabRoute].
 * [siteId] is `Site.analyticsId`, the id used in `/api/sites/{site}/...` paths.
 */
@Serializable
data class SiteRoute(val siteId: String, val siteName: String)

/** Inner tab routes. Only valid inside [SiteRoute]; the site id comes from the parent route. */
@Serializable
data object OverviewTabRoute

@Serializable
data object SessionsTabRoute

@Serializable
data object EventsTabRoute

@Serializable
data object UsersTabRoute

@Serializable
data object MoreTabRoute

/* Detail screens. They live on the outer NavHost, so they are full screen with no tab bar. */

@Serializable
data class SessionDetailRoute(val siteId: String, val sessionId: String)

@Serializable
data class UserDetailRoute(val siteId: String, val userId: String)

@Serializable
data class EventDetailRoute(val siteId: String, val eventName: String)

@Serializable
data class ErrorDetailRoute(val siteId: String, val errorMessage: String)

@Serializable
data class GoalsRoute(val siteId: String)

@Serializable
data class FunnelsRoute(val siteId: String)

@Serializable
data class FunnelDetailRoute(val siteId: String, val funnelId: Int)

@Serializable
data class RetentionRoute(val siteId: String)

@Serializable
data class ErrorsRoute(val siteId: String)

@Serializable
data class RealtimeRoute(val siteId: String)

@Serializable
data class WebVitalsRoute(val siteId: String)
