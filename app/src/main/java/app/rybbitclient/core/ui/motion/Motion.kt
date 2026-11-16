package app.rybbitclient.core.ui.motion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.rybbitclient.core.ui.theme.LocalReduceMotion

/**
 * Whether the user allows animation. Compose finite animations already snap when the system
 * "Remove animations" setting zeroes the animator scale, but endless loops (the live pulse, the
 * shimmer) must stop themselves, otherwise they keep running and keep the frame clock busy.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    val reduceMotion = LocalReduceMotion.current
    return remember(context, reduceMotion) {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        scale != 0f && !reduceMotion
    }
}
