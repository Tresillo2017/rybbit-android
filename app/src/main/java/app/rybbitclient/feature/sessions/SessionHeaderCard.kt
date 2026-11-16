package app.rybbitclient.feature.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.model.SessionDetail
import app.rybbitclient.core.network.RybbitTimestamps
import app.rybbitclient.core.ui.components.SectionCard
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.countryLabel
import app.rybbitclient.core.ui.format.formatDuration
import app.rybbitclient.core.ui.theme.Spacing
import java.time.ZoneId

private const val VIEW_USER_LABEL = "View user"
private const val SECTION_TITLE = "Session"
private val LABEL_WIDTH = 104.dp

/** One labelled line of the header card. */
data class HeaderRow(val label: String, val value: String)

/**
 * The session header: visitor, location, device, browser, OS, screen, language, referrer, channel, entry and
 * exit pages, start and end in [zone], and duration. "View user" opens the visitor's profile.
 */
@Composable
fun SessionHeaderCard(
    session: SessionDetail,
    zone: ZoneId,
    onViewUser: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val userId = session.userId?.takeIf(String::isNotBlank)
    SectionCard(title = SECTION_TITLE, modifier = modifier) {
        sessionHeaderRows(session, zone).forEach { row -> HeaderLine(row) }
        if (userId != null) {
            Button(
                onClick = { onViewUser(userId) },
                modifier = Modifier.padding(top = Spacing.sm),
            ) {
                Text(VIEW_USER_LABEL)
            }
        }
    }
}

/** One label/value line, shared by the session and user profile cards. */
@Composable
internal fun HeaderLine(row: HeaderRow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(LABEL_WIDTH),
        )
        Text(
            text = row.value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
    }
}

/** The header rows in display order. Blank values show an em dash. Pure, so it can be unit tested. */
internal fun sessionHeaderRows(session: SessionDetail, zone: ZoneId): List<HeaderRow> {
    val started = RybbitTimestamps.parseUtcOrNull(session.sessionStart)
    val ended = RybbitTimestamps.parseUtcOrNull(session.sessionEnd)
    return listOf(
        HeaderRow("Visitor", session.userId.orDash()),
        HeaderRow("Location", locationLabel(session.city, session.region, countryText(session.country))),
        HeaderRow("Device", session.deviceType.orDash()),
        HeaderRow("Browser", browserLabel(session.browser, session.browserVersion)),
        HeaderRow("System", operatingSystemLabel(session.operatingSystem, session.operatingSystemVersion)),
        HeaderRow("Screen", screenLabel(session.screenWidth, session.screenHeight)),
        HeaderRow("Language", session.language.orDash()),
        HeaderRow("Referrer", session.referrer.orDash()),
        HeaderRow("Channel", session.channel.orDash()),
        HeaderRow("Entry page", session.entryPage.orDash()),
        HeaderRow("Exit page", session.exitPage.orDash()),
        HeaderRow("Started", started?.let { formatLocalDateTime(it, zone) } ?: EMPTY_VALUE),
        HeaderRow("Ended", ended?.let { formatLocalDateTime(it, zone) } ?: EMPTY_VALUE),
        HeaderRow("Duration", formatDuration(session.sessionDuration ?: 0.0)),
    )
}

/** Flag and localized name for a country code, or null when there is none. */
private fun countryText(code: String?): String? = code?.takeIf(String::isNotBlank)?.let { countryLabel(it) }
