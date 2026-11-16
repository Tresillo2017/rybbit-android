package app.rybbitclient.core.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * The in-app "Reduce motion" switch. When true, endless loops stop, entrances appear instantly and screens fade
 * instead of sliding. Independent of, and additive to, Android's system "Remove animations" setting.
 */
val LocalReduceMotion = compositionLocalOf { false }

/** Whether line charts draw smooth curves (true) or straight segments between points (false). */
val LocalSmoothCharts = compositionLocalOf { true }
