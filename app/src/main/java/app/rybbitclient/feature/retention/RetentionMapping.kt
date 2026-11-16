package app.rybbitclient.feature.retention

import app.rybbitclient.core.model.RetentionCohort as CohortData
import app.rybbitclient.core.model.RetentionData
import app.rybbitclient.core.ui.components.RetentionCohort
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val ISO_DATE_LENGTH = 10
private const val COHORT_LABEL_PATTERN = "d MMM yyyy"

/**
 * Cohort rows for the heatmap, oldest first. The API keys cohorts by ISO date, which sorts correctly as text.
 * A cohort without a size is shown as 0 users.
 */
fun cohortRows(data: RetentionData, locale: Locale = Locale.getDefault()): List<RetentionCohort> =
    data.cohorts.entries
        .sortedBy { it.key }
        .map { (date, cohort) -> cohortRow(date, cohort, locale) }

private fun cohortRow(date: String, cohort: CohortData, locale: Locale): RetentionCohort =
    RetentionCohort(
        label = cohortDateLabel(date, locale),
        size = cohort.size ?: 0,
        percentages = cohort.percentages,
    )

/** "16 Nov 2026" for an ISO cohort key. Anything that is not a plain date is shown as the raw key. */
fun cohortDateLabel(key: String, locale: Locale = Locale.getDefault()): String =
    runCatching {
        LocalDate.parse(key.take(ISO_DATE_LENGTH))
            .format(DateTimeFormatter.ofPattern(COHORT_LABEL_PATTERN, locale))
    }.getOrDefault(key)
