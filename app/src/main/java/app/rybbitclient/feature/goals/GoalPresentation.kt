package app.rybbitclient.feature.goals

import app.rybbitclient.core.model.Goal
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val PATH_PATTERN_KEY = "pathPattern"
private const val EVENT_NAME_KEY = "eventName"

/** The two goal kinds Rybbit defines, plus a fallback for kinds this app does not know yet. */
enum class GoalKind(val badge: String) {
    Path("Path"),
    Event("Event"),
    Other("Goal"),
}

fun goalKindOf(goalType: String?): GoalKind = when (goalType?.lowercase()) {
    "path" -> GoalKind.Path
    "event" -> GoalKind.Event
    else -> GoalKind.Other
}

/** The page pattern or event name a goal tracks, read from its type-specific `config`. Null when absent. */
fun goalTargetOf(goal: Goal): String? {
    val config = goal.config as? JsonObject ?: return null
    val key = when (goalKindOf(goal.goalType)) {
        GoalKind.Path -> PATH_PATTERN_KEY
        GoalKind.Event -> EVENT_NAME_KEY
        GoalKind.Other -> return null
    }
    return (config[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}

/**
 * Conversion as a fraction from 0 to 1. The server's `conversion_rate` is a fraction (0.625 is 62.5%),
 * unlike the percent units used elsewhere. Falls back to the totals when the rate is missing.
 */
fun goalConversionFraction(goal: Goal): Double? {
    goal.conversionRate?.let { return it.coerceIn(0.0, 1.0) }
    val conversions = goal.totalConversions ?: return null
    val sessions = goal.totalSessions?.takeIf { it > 0 } ?: return null
    return (conversions.toDouble() / sessions).coerceIn(0.0, 1.0)
}
