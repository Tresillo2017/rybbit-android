package app.rybbitclient.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import app.rybbitclient.core.ui.motion.rememberAnimationsEnabled
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape

private const val SHIMMER_SWEEP_MILLIS = 1_400

/**
 * Animated skeleton placeholder: a moving highlight over a surface-variant block.
 * Pair with a fixed size so the layout does not shift when real content arrives.
 */
@Composable
fun Modifier.shimmerPlaceholder(shape: Shape): Modifier {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    // With "Remove animations" on, the loop must not run: show the flat base colour instead.
    if (!rememberAnimationsEnabled()) return this.clip(shape).background(base)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_SWEEP_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    return this
        .clip(shape)
        .background(base)
        .drawBehind {
            val width = size.width
            val centre = width * progress
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(centre - width, 0f),
                    end = Offset(centre, 0f),
                ),
            )
        }
}
