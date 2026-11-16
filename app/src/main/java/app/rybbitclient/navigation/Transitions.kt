package app.rybbitclient.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import app.rybbitclient.core.ui.theme.LocalReduceMotion

/** A navigation transition factory as consumed by `NavHost` and `composable`. */
typealias EnterSpec = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
typealias ExitSpec = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

/** Distance, as a fraction of the screen width, that the incoming/outgoing page travels. */
private const val FORWARD_ENTER_FRACTION = 4
private const val FORWARD_EXIT_FRACTION = 8

/**
 * Screen-to-screen transitions for parent -> child navigation (the "forward and backward" pattern):
 * the child slides in from the trailing edge on a spatial spring while fading in on an effects spring,
 * the parent drifts back a short way and fades. Back reverses it. Because the specs come from the theme's
 * motion scheme, the whole app changes feel by swapping the scheme, and predictive back seeks the same specs.
 */
@Immutable
class ForwardBackTransitions(
    val enter: EnterSpec,
    val exit: ExitSpec,
    val popEnter: EnterSpec,
    val popExit: ExitSpec,
)

@Composable
fun rememberForwardBackTransitions(): ForwardBackTransitions {
    val motion = MaterialTheme.motionScheme
    val reduceMotion = LocalReduceMotion.current
    return remember(motion, reduceMotion) {
        val slide = motion.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
        val fadeSpec = motion.defaultEffectsSpec<Float>()
        if (reduceMotion) return@remember ForwardBackTransitions(
            enter = { fadeIn(animationSpec = fadeSpec) },
            exit = { fadeOut(animationSpec = fadeSpec) },
            popEnter = { fadeIn(animationSpec = fadeSpec) },
            popExit = { fadeOut(animationSpec = fadeSpec) },
        )
        ForwardBackTransitions(
            enter = {
                slideInHorizontally(animationSpec = slide) { it / FORWARD_ENTER_FRACTION } +
                    fadeIn(animationSpec = fadeSpec)
            },
            exit = {
                slideOutHorizontally(animationSpec = slide) { -it / FORWARD_EXIT_FRACTION } +
                    fadeOut(animationSpec = fadeSpec)
            },
            popEnter = {
                slideInHorizontally(animationSpec = slide) { -it / FORWARD_EXIT_FRACTION } +
                    fadeIn(animationSpec = fadeSpec)
            },
            popExit = {
                slideOutHorizontally(animationSpec = slide) { it / FORWARD_ENTER_FRACTION } +
                    fadeOut(animationSpec = fadeSpec)
            },
        )
    }
}

/** Cross-fade for unordered top-level destinations (the site tabs): no spatial relationship, so no slide. */
@Composable
fun rememberTabFade(): Pair<EnterSpec, ExitSpec> {
    val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    return remember(fadeSpec) {
        val enter: EnterSpec = { fadeIn(animationSpec = fadeSpec) }
        val exit: ExitSpec = { fadeOut(animationSpec = fadeSpec) }
        enter to exit
    }
}
