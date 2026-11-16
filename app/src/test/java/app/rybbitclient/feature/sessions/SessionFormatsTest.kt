package app.rybbitclient.feature.sessions

import app.rybbitclient.core.model.SessionDetail
import app.rybbitclient.core.model.SessionSummary
import app.rybbitclient.core.model.UserDetail
import app.rybbitclient.core.model.UserDevice
import app.rybbitclient.core.model.UserLocation
import app.rybbitclient.core.model.UserSummary
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.query.FilterType
import app.rybbitclient.feature.users.traitRows
import app.rybbitclient.feature.users.userProfileRows
import app.rybbitclient.feature.users.userSupportingLine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class SessionFormatsTest {

    @Test
    fun `location joins the known parts and falls back to a dash`() {
        assertEquals("Harlesden, GB-ENG, GB", locationLabel("Harlesden", "GB-ENG", "GB"))
        assertEquals("GB", locationLabel(null, "", "GB"))
        assertEquals("—", locationLabel(null, null, null))
    }

    @Test
    fun `browser and system labels include the version when present`() {
        assertEquals("Safari 17", browserLabel("Safari", "17"))
        assertEquals("iOS", operatingSystemLabel("iOS", ""))
        assertEquals("—", browserLabel(null, null))
    }

    @Test
    fun `anonymous users show a short ID and identified users show their ID`() {
        assertEquals("Anonymous db8f8c98", userDisplayName("db8f8c98c762", identifiedUserId = ""))
        assertEquals("ada@example.com", userDisplayName("db8f8c98c762", identifiedUserId = "ada@example.com"))
    }

    @Test
    fun `session metrics pluralize and skip unknown parts`() {
        assertEquals("1 pageview · 1 event · 1m 05s", sessionMetricsLine(1, 1, 65.0))
        assertEquals("3 pageviews · 0 events", sessionMetricsLine(3, 0, null))
        assertEquals("—", sessionMetricsLine(null, null, null))
    }

    @Test
    fun `headline shows the flag, entry and exit pages`() {
        val session = SessionSummary(sessionId = "s1", country = "DE", entryPage = "/signup", exitPage = "/docs")

        assertEquals("🇩🇪 /signup → /docs", sessionHeadline(session))
    }

    @Test
    fun `a custom range from picker millis swaps reversed dates into one day`() {
        val start = LocalDate.of(2026, 11, 3).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 11, 9).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        assertEquals(DateRange.Custom(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 9)), customRangeFromMillis(start, end))
        assertEquals(DateRange.Custom(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 9)), customRangeFromMillis(end, start))
    }

    @Test
    fun `presets round-trip through their keys and custom ranges have no key`() {
        assertEquals("7d", presetKeyFor(DateRange.Last7Days))
        assertEquals(DateRange.Last30Days, rangeForPreset("30d"))
        assertNull(presetKeyFor(DateRange.Custom(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2))))
        assertNull(rangeForPreset("unknown"))
    }

    @Test
    fun `filter chips carry a stable key and a readable label`() {
        val filter = Filter(parameter = "browser", type = FilterType.Equals, values = listOf("Firefox", "Chrome"))
        val negated = Filter(parameter = "country", type = FilterType.NotEquals, values = listOf("US"))

        assertEquals("Browser: Firefox, Chrome", filterLabel(filter))
        assertEquals("Country not equals: US", filterLabel(negated))
        assertEquals(filterKey(filter), activeFilterChips(listOf(filter)).single().key)
    }

    @Test
    fun `session header rows show local start time and the em dash for missing values`() {
        val session = SessionDetail(
            sessionId = "s1",
            userId = "device-1",
            sessionStart = "2026-11-16 08:41:49",
            sessionEnd = null,
            sessionDuration = 65.0,
        )

        val rows = sessionHeaderRows(session, ZoneId.of("Europe/Madrid")).associateBy { it.label }

        assertTrue(rows.getValue("Started").value.endsWith("09:41:49"))
        assertEquals("—", rows.getValue("Ended").value)
        assertEquals("1m 05s", rows.getValue("Duration").value)
        assertEquals("—", rows.getValue("Screen").value)
    }

    @Test
    fun `user profile rows use the first location and device`() {
        val profile = UserDetail(
            userId = "device-1",
            sessions = 2,
            pageviews = 1,
            duration = 0,
            country = "US",
            locations = listOf(UserLocation(country = "US", city = "Austin")),
            devices = listOf(UserDevice(deviceType = "Desktop", browser = "Safari", browserVersion = "17", operatingSystem = "macOS")),
        )

        val rows = userProfileRows(profile, ZoneId.of("UTC")).associateBy { it.label }

        assertTrue(rows.getValue("Location").value.startsWith("Austin, "))
        assertEquals("Desktop · Safari 17 · macOS", rows.getValue("Device").value)
        assertEquals("2 sessions", rows.getValue("Sessions").value)
    }

    @Test
    fun `traits become rows only when they are an object`() {
        val traits = JsonObject(mapOf("plan" to JsonPrimitive("pro"), "seats" to JsonPrimitive(4)))

        assertEquals(listOf("plan" to "pro", "seats" to "4"), traitRows(traits).map { it.label to it.value })
        assertTrue(traitRows(JsonPrimitive("not an object")).isEmpty())
        assertTrue(traitRows(null).isEmpty())
    }

    @Test
    fun `user supporting line lists country, browser and pageviews`() {
        val user = UserSummary(userId = "u1", country = "BR", browser = "Firefox", browserVersion = "133", pageviews = 12)

        assertTrue(userSupportingLine(user).endsWith("Firefox 133 · 12 pageviews"))
    }
}
