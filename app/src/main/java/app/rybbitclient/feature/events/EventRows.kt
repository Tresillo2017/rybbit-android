package app.rybbitclient.feature.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.rybbitclient.core.model.Event
import app.rybbitclient.core.ui.format.countryLabel
import app.rybbitclient.core.ui.theme.Spacing

private const val CUSTOM_EVENT_TYPE = "custom_event"
private const val DETAIL_SEPARATOR = " · "

/** The event name when the row is a named custom event, which can open EventDetail. Null otherwise. */
fun Event.openableEventName(): String? = eventName?.takeIf { type == CUSTOM_EVENT_TYPE && it.isNotBlank() }

/** Headline for a row: the custom event name, else the page path, else the page title, else the type. */
fun Event.headline(): String =
    openableEventName() ?: pathname?.takeIf { it.isNotBlank() } ?: pageTitle?.takeIf { it.isNotBlank() } ?: typeLabel()

/** Human label for `Event.type`, e.g. "Custom event" for `custom_event`. */
fun Event.typeLabel(): String = when (type) {
    null, "" -> "Event"
    "pageview" -> "Pageview"
    "custom_event" -> "Custom event"
    "outbound" -> "Outbound link"
    "error" -> "Error"
    else -> type.replace('_', ' ').replaceFirstChar { it.titlecase() }
}

/** One row of an event list: headline, relative time, and a line of context (type, browser, OS, country). */
@Composable
fun EventRow(
    event: Event,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget)
            .then(clickModifier)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = event.headline(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = relativeEventTime(event.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
        Text(
            text = contextLine(event),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun contextLine(event: Event): String {
    val parts = listOfNotNull(
        event.typeLabel(),
        event.browser?.takeIf { it.isNotBlank() },
        event.operatingSystem?.takeIf { it.isNotBlank() },
        event.country?.takeIf { it.isNotBlank() }?.let { countryLabel(it) },
    )
    return parts.joinToString(separator = DETAIL_SEPARATOR)
}
