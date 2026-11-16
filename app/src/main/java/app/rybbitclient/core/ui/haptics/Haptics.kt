package app.rybbitclient.core.ui.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Semantic haptic vocabulary for the whole app, so screens say what happened (a selection moved,
 * a connection succeeded) and the mapping to the platform's richer API 30+ effects lives in one place.
 * The platform already honours the user's "Touch feedback" system setting; nothing fires when it is off.
 */
@Immutable
class Haptics(private val feedback: HapticFeedback, private val enabled: Boolean = true) {
    private fun perform(type: HapticFeedbackType) {
        if (enabled) feedback.performHapticFeedback(type)
    }

    /** A discrete selection moved: tabs, chips, segmented choices. */
    fun selection() = perform(HapticFeedbackType.SegmentTick)

    /** Light tick while scrubbing across many points, e.g. a chart. Quieter than [selection]. */
    fun scrubTick() = perform(HapticFeedbackType.SegmentFrequentTick)

    /** A switch or checkbox changed. */
    fun toggle(isOn: Boolean) = perform(if (isOn) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

    /** A drag crossed the point where releasing will commit, e.g. pull-to-refresh. */
    fun thresholdReached() = perform(HapticFeedbackType.GestureThresholdActivate)

    /** A gesture finished and was accepted. */
    fun gestureEnd() = perform(HapticFeedbackType.GestureEnd)

    /** An action completed successfully, e.g. connecting to a server. */
    fun confirm() = perform(HapticFeedbackType.Confirm)

    /** An action failed or was refused. */
    fun reject() = perform(HapticFeedbackType.Reject)

    /** A press on a row or card that navigates. */
    fun click() = perform(HapticFeedbackType.ContextClick)

    /** A press-and-hold was recognised. */
    fun longPress() = perform(HapticFeedbackType.LongPress)
}

/** Whether the app plays haptics at all; set from the Settings switch at the root of the tree. */
val LocalHapticsEnabled = compositionLocalOf { true }

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(feedback, enabled) { Haptics(feedback, enabled) }
}
