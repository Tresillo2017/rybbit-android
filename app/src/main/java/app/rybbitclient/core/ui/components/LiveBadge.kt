package app.rybbitclient.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.rybbitclient.core.ui.motion.rememberAnimationsEnabled
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.rybbitColors

private const val PULSE_MILLIS = 1_600
private const val DOT_SIZE_DP = 8
private const val HALO_SIZE_DP = 18
private const val HALO_MAX_SCALE = 1.0f
private const val HALO_MIN_SCALE = 0.35f
private const val HALO_MAX_ALPHA = 0.55f
private const val BADGE_CORNER_PERCENT = 50

/** Resting halo when animations are off: a faint, steady ring so the badge still reads as "live". */
private const val STATIC_PULSE = 0.5f

/**
 * "12 online" pill with a pulsing green dot. Pass `null` while the count is loading; it shows a dash.
 * [suffix] is the trailing word, e.g. "online" or "visitors".
 */
@Composable
fun LiveBadge(
    count: Int?,
    modifier: Modifier = Modifier,
    suffix: String = "online",
) {
    val countText = count?.let { formatFullNumber(it.toDouble()) } ?: EMPTY_VALUE
    val description = "$countText $suffix right now"
    Surface(
        modifier = modifier.semantics { contentDescription = description },
        shape = RoundedCornerShape(percent = BADGE_CORNER_PERCENT),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulsingDot(color = MaterialTheme.rybbitColors.live)
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = "$countText $suffix",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PulsingDot(color: androidx.compose.ui.graphics.Color) {
    val animate = rememberAnimationsEnabled()
    val pulse = if (animate) {
        val transition = rememberInfiniteTransition(label = "livePulse")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = PULSE_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "livePulseProgress",
        )
        value
    } else {
        STATIC_PULSE
    }
    Box(modifier = Modifier.size(HALO_SIZE_DP.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(HALO_SIZE_DP.dp)) {
            val maxRadius = size.minDimension / 2f
            val scale = HALO_MIN_SCALE + (HALO_MAX_SCALE - HALO_MIN_SCALE) * pulse
            drawCircle(
                color = color.copy(alpha = HALO_MAX_ALPHA * (1f - pulse)),
                radius = maxRadius * scale,
            )
            drawCircle(color = color, radius = DOT_SIZE_DP.dp.toPx() / 2f)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveBadgePreview() {
    RybbitTheme {
        Row(modifier = Modifier.padding(Spacing.lg)) {
            LiveBadge(count = 12)
            Spacer(Modifier.width(Spacing.sm))
            LiveBadge(count = null, suffix = "visitors")
        }
    }
}
