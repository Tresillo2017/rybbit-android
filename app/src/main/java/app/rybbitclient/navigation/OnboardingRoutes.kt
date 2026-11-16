package app.rybbitclient.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import app.rybbitclient.feature.onboarding.WelcomeScreen

/**
 * First-run setup graph. Start destination: [WelcomeRoute]. Adding an instance (AddInstanceRoute) is a
 * root-level route so Settings can reuse it without pulling this graph onto the back stack.
 */
fun NavGraphBuilder.onboardingRoutes(navigator: AppNavigator) {
    navigation<OnboardingGraphRoute>(startDestination = WelcomeRoute::class) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onAddCloud = { navigator.openAddInstance(isCloud = true, fromOnboarding = true) },
                onAddSelfHosted = { navigator.openAddInstance(isCloud = false, fromOnboarding = true) },
            )
        }
    }
}
