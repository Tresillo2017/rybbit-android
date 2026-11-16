package app.rybbitclient.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale on a 4dp grid. Use these instead of raw dp literals. */
object Spacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    /** Horizontal page gutter; matches the 16dp phone gutter in the architecture contract. */
    val screenGutter: Dp = 16.dp

    /** Minimum touch target per accessibility guidelines. */
    val minTouchTarget: Dp = 48.dp
}
