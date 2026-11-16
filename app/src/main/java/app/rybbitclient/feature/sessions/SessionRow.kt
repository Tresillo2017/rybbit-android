package app.rybbitclient.feature.sessions

import app.rybbitclient.core.ui.components.hapticClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.DeviceUnknown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.model.SessionSummary
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.countryFlagEmoji
import app.rybbitclient.core.ui.format.formatDuration
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.format.formatRelativeTime
import app.rybbitclient.core.ui.theme.Spacing

private const val ICON_SIZE_DP = 16
private const val METRIC_SEPARATOR = " · "
private val BADGE_CORNER = RoundedCornerShape(50)

/**
 * One session: flag, entry → exit page, browser/OS/device icons, counts and duration, relative start time,
 * and an "Identified" badge when the session belongs to an identified user.
 */
@Composable
fun SessionRow(session: SessionSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = sessionHeadline(session), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                TechLine(session)
                Text(
                    text = sessionMetricsLine(session.pageviews, session.events, session.sessionDuration),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        trailingContent = { SessionTrailing(session) },
        modifier = Modifier.hapticClickable(onClick = onClick),
    )
}

/** Device, browser and OS, each with an icon that screen readers describe. */
@Composable
private fun TechLine(session: SessionSummary) {
    val browser = browserLabel(session.browser, session.browserVersion)
    val system = operatingSystemLabel(session.operatingSystem, session.operatingSystemVersion)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TechIcon(deviceIcon(session.deviceType), "Device ${session.deviceType.orDash()}")
        Spacer(Modifier.width(Spacing.xs))
        TechIcon(Icons.Outlined.Language, "Browser $browser")
        Spacer(Modifier.width(Spacing.xs))
        TechIcon(Icons.Outlined.DesktopWindows, "Operating system $system")
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = browser + METRIC_SEPARATOR + system,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TechIcon(icon: ImageVector, description: String) {
    Icon(imageVector = icon, contentDescription = description, modifier = Modifier.size(ICON_SIZE_DP.dp))
}

@Composable
private fun SessionTrailing(session: SessionSummary) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        val started = utcMillisOrNull(session.sessionStart)
        Text(
            text = started?.let { formatRelativeTime(it) } ?: EMPTY_VALUE,
            style = MaterialTheme.typography.labelMedium,
        )
        if (!session.identifiedUserId.isNullOrBlank()) {
            IdentifiedBadge()
        }
    }
}

@Composable
private fun IdentifiedBadge() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = BADGE_CORNER) {
        Text(
            text = "Identified",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        )
    }
}

/** Entry → exit, with the country flag in front when the country is known. */
internal fun sessionHeadline(session: SessionSummary): String {
    val flag = session.country?.let(::countryFlagEmoji).orEmpty()
    val route = "${session.entryPage.orDash()} → ${session.exitPage.orDash()}"
    return listOf(flag, route).filter(String::isNotBlank).joinToString(" ")
}

/** `3 pageviews · 1 event · 1m 05s`; parts that are unknown are left out. */
internal fun sessionMetricsLine(pageviews: Int?, events: Int?, durationSeconds: Double?): String {
    val parts = listOfNotNull(
        pageviews?.let { plural(it, "pageview") },
        events?.let { plural(it, "event") },
        durationSeconds?.let { formatDuration(it) },
    )
    return parts.ifEmpty { listOf(EMPTY_VALUE) }.joinToString(METRIC_SEPARATOR)
}

internal fun deviceIcon(deviceType: String?): ImageVector = when (deviceType?.lowercase()) {
    "desktop" -> Icons.Outlined.Computer
    "mobile" -> Icons.Outlined.Smartphone
    "tablet" -> Icons.Outlined.Tablet
    else -> Icons.Outlined.DeviceUnknown
}

internal fun plural(count: Int, word: String): String =
    "${formatFullNumber(count.toDouble())} $word" + if (count == 1) "" else "s"
