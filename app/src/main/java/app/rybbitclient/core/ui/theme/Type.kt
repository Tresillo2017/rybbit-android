package app.rybbitclient.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** OpenType tabular figures: digits share one width so columns of numbers align. */
const val TabularNumbers = "tnum"

/** Default platform families with a tuned scale: tighter display, heavier titles. */
fun rybbitTypography(): Typography {
    val base = Typography()
    return base.copy(
        displaySmall = base.displaySmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
            fontFeatureSettings = TabularNumbers,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.25).sp,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = TabularNumbers,
        ),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyMedium = base.bodyMedium.copy(letterSpacing = 0.1.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TabularNumbers),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
    )
}

/** Big KPI number, e.g. "12.4k" on a [app.rybbitclient.core.ui.components.KpiCard]. */
val Typography.metricValue: TextStyle
    get() = headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TabularNumbers)

/** Compact delta badge text such as "+12.3%". */
val Typography.metricDelta: TextStyle
    get() = labelMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TabularNumbers)

/** Table / list value column: body text with tabular digits. */
val Typography.tabularBody: TextStyle
    get() = bodyMedium.copy(fontFeatureSettings = TabularNumbers)

/** Small uppercase-ish section overline used above KPI labels and card headers. */
val Typography.overline: TextStyle
    get() = labelSmall.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold)
