package app.rybbitclient.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.rybbitclient.di.LocalAppContainer

/**
 * Root navigation. Starts at [SplashRoute], which decides between onboarding and the site picker
 * once the instance list has loaded. Requires [LocalAppContainer] to be provided.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val container = LocalAppContainer.current
    val navigator = remember(navController) { AppNavigator(navController) }

    NavHost(navController = navController, startDestination = SplashRoute, modifier = modifier) {
        composable<SplashRoute> {
            StartupRoute(
                instances = container.instanceRepository.instances,
                onReady = navigator::leaveStartup,
            )
        }
        onboardingRoutes(navigator)
        rootRoutes(navigator)
        detailRoutes(navigator)
    }
}
