package app.rybbitclient.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

/** The five bottom-bar tabs of a site, in display order. */
enum class SiteTab(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val routeClass: KClass<*>,
) {
    Overview("Overview", Icons.Outlined.Dashboard, OverviewTabRoute, OverviewTabRoute::class),
    Sessions("Sessions", Icons.Outlined.History, SessionsTabRoute, SessionsTabRoute::class),
    Events("Events", Icons.Outlined.Bolt, EventsTabRoute, EventsTabRoute::class),
    Users("Users", Icons.Outlined.Person, UsersTabRoute, UsersTabRoute::class),
    More("More", Icons.Outlined.MoreHoriz, MoreTabRoute, MoreTabRoute::class),
}
