package app.rybbitclient.feature.users

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rybbitclient.core.model.UserDetail
import app.rybbitclient.core.network.RybbitTimestamps
import app.rybbitclient.core.ui.components.SectionCard
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.countryLabel
import app.rybbitclient.core.ui.format.formatDuration
import app.rybbitclient.feature.sessions.HeaderLine
import app.rybbitclient.feature.sessions.HeaderRow
import app.rybbitclient.feature.sessions.browserLabel
import app.rybbitclient.feature.sessions.formatLocalDateTime
import app.rybbitclient.feature.sessions.locationLabel
import app.rybbitclient.feature.sessions.operatingSystemLabel
import app.rybbitclient.feature.sessions.orDash
import app.rybbitclient.feature.sessions.plural
import app.rybbitclient.feature.sessions.screenLabel
import app.rybbitclient.feature.sessions.userDisplayName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.ZoneId

private const val PROFILE_TITLE = "Profile"
private const val TRAITS_TITLE = "Traits"
private const val MIDDOT = " · "

/** Profile card for a user: identity, totals, first and last seen, where and on what, and identified traits. */
@Composable
fun UserProfileCard(profile: UserDetail, zone: ZoneId, modifier: Modifier = Modifier) {
    SectionCard(title = PROFILE_TITLE, modifier = modifier) {
        userProfileRows(profile, zone).forEach { row -> HeaderLine(row) }
        val traits = traitRows(profile.traits)
        if (traits.isNotEmpty()) {
            SectionCard(title = TRAITS_TITLE) {
                traits.forEach { row -> HeaderLine(row) }
            }
        }
    }
}

/** Profile rows in display order. Pure, so it can be unit tested. */
internal fun userProfileRows(profile: UserDetail, zone: ZoneId): List<HeaderRow> {
    val firstSeen = RybbitTimestamps.parseUtcOrNull(profile.firstSeen)
    val lastSeen = RybbitTimestamps.parseUtcOrNull(profile.lastSeen)
    val location = profile.locations.firstOrNull()
    val device = profile.devices.firstOrNull()
    return listOf(
        HeaderRow("Visitor", userDisplayName(profile.userId, profile.identifiedUserId)),
        HeaderRow("Sessions", profile.sessions?.let { plural(it, "session") } ?: EMPTY_VALUE),
        HeaderRow("Pageviews", profile.pageviews?.let { plural(it, "pageview") } ?: EMPTY_VALUE),
        HeaderRow("Avg session", formatDuration((profile.duration ?: 0).toLong())),
        HeaderRow("First seen", firstSeen?.let { formatLocalDateTime(it, zone) } ?: EMPTY_VALUE),
        HeaderRow("Last seen", lastSeen?.let { formatLocalDateTime(it, zone) } ?: EMPTY_VALUE),
        HeaderRow(
            "Location",
            location?.let { locationLabel(it.city, it.region, profile.country?.takeIf(String::isNotBlank)?.let { code -> countryLabel(code) }) }
                ?: profile.country.orDash(),
        ),
        HeaderRow("Device", deviceLine(device?.deviceType, device?.browser, device?.browserVersion, device?.operatingSystem, device?.operatingSystemVersion)),
        HeaderRow("Screen", screenLabel(device?.screenWidth, device?.screenHeight)),
        HeaderRow("First channel", profile.firstChannel.orDash()),
        HeaderRow("First referrer", profile.firstReferrer.orDash()),
        HeaderRow("First entry page", profile.firstEntryPage.orDash()),
        HeaderRow("Language", profile.language.orDash()),
    )
}

/** Trait key/value rows. Traits that are not a JSON object produce no rows. */
internal fun traitRows(traits: JsonElement?): List<HeaderRow> =
    (traits as? JsonObject)?.map { (key, value) -> HeaderRow(key, traitText(value)) }.orEmpty()

private fun traitText(value: JsonElement): String = (value as? JsonPrimitive)?.contentOrNull ?: value.toString()

private fun deviceLine(
    type: String?,
    browser: String?,
    browserVersion: String?,
    system: String?,
    systemVersion: String?,
): String {
    val parts = listOf(
        type?.takeIf(String::isNotBlank),
        browserLabel(browser, browserVersion).takeIf { it != EMPTY_VALUE },
        operatingSystemLabel(system, systemVersion).takeIf { it != EMPTY_VALUE },
    )
    return parts.filterNotNull().joinToString(MIDDOT).ifEmpty { EMPTY_VALUE }
}
