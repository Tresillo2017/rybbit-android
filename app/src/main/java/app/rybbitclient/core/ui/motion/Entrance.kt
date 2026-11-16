package app.rybbitclient.core.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val STAGGER_STEP_MILLIS = 70L
private const val RISE_DP = 56

/**
 * One-time entrance for first-run and other rare surfaces: the element rises into place on a slow spatial
 * spring (so it settles with a gentle overshoot) while fading in on an effects spring. [index] staggers
 * siblings. Do not use on frequently-seen screens or data rows; the dashboard itself stays still.
 * Both values are read in the draw phase, and with animations removed the element simply appears.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    val motion = MaterialTheme.motionScheme
    val spatial = motion.slowSpatialSpec<Float>()
    val effects = motion.slowEffectsSpec<Float>()
    val rise = remember { Animatable(1f) }
    val alpha = remember { Animatable(0f) }
    val animate = rememberAnimationsEnabled()
    LaunchedEffect(animate) {
        if (!animate) {
            rise.snapTo(0f)
            alpha.snapTo(1f)
            return@LaunchedEffect
        }
        delay(index * STAGGER_STEP_MILLIS)
        coroutineScope {
            launch { rise.animateTo(0f, spatial) }
            launch { alpha.animateTo(1f, effects) }
        }
    }
    return graphicsLayer {
        this.alpha = alpha.value
        translationY = rise.value * RISE_DP.dp.toPx()
    }
}
