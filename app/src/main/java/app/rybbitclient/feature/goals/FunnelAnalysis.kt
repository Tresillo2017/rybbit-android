package app.rybbitclient.feature.goals

import app.rybbitclient.core.model.Funnel
import app.rybbitclient.core.model.FunnelStepResult
import app.rybbitclient.core.ui.components.FunnelStepItem

private const val PERCENT_SCALE = 100.0
private const val MAX_STEP_PREVIEW = 3
private const val STEP_SEPARATOR = " → "
private const val ELLIPSIS = "…"

/**
 * An analyzed funnel in chart terms. Percentages are 0..100, as the server returns them. The overall
 * conversion is the share of entering visitors who reached the last step.
 */
data class FunnelAnalysis(
    val steps: List<FunnelStepItem>,
    val enteredVisitors: Int?,
    val completedVisitors: Int?,
    val overallConversionPercent: Double?,
)

/** Maps the `POST /funnels/analyze` rows to chart steps and the overall summary. Empty input stays empty. */
fun analyzeFunnelResults(results: List<FunnelStepResult>): FunnelAnalysis {
    val steps = results.mapIndexed { index, result ->
        FunnelStepItem(
            label = result.stepName ?: "Step ${result.stepNumber ?: index + 1}",
            count = visitorsOf(result).toDouble(),
            conversionPercent = result.conversionRate,
            dropoffPercent = result.dropoffRate,
        )
    }
    val entered = results.firstOrNull()?.let(::visitorsOf)
    val completed = results.lastOrNull()?.let(::visitorsOf)
    val overall = results.lastOrNull()?.conversionRate ?: shareOf(completed, entered)
    return FunnelAnalysis(steps = steps, enteredVisitors = entered, completedVisitors = completed, overallConversionPercent = overall)
}

/** Short preview of a saved funnel's steps, such as "Home → Pricing → Signup…". */
fun funnelStepPreview(funnel: Funnel): String {
    val names = funnel.steps.map { step -> step.name?.takeIf { it.isNotBlank() } ?: step.value }
    val shown = names.take(MAX_STEP_PREVIEW).joinToString(STEP_SEPARATOR)
    return if (names.size > MAX_STEP_PREVIEW) shown + STEP_SEPARATOR + ELLIPSIS else shown
}

private fun visitorsOf(result: FunnelStepResult): Int = result.visitors ?: result.sessions ?: 0

private fun shareOf(part: Int?, whole: Int?): Double? {
    if (part == null || whole == null || whole <= 0) return null
    return part.toDouble() / whole * PERCENT_SCALE
}
