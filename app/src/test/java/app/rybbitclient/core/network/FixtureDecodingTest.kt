package app.rybbitclient.core.network

import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.TimeBucket
import app.rybbitclient.testsupport.FixtureFiles
import app.rybbitclient.testsupport.apiReplying
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** Decodes real server responses (docs/fixtures) through the real RybbitApi methods. */
class FixtureDecodingTest {

    private val query = AnalyticsQuery(DateRange.AllTime, zone = ZoneId.of("Europe/Madrid"))

    private fun api(fixture: String) = apiReplying(FixtureFiles.read(fixture))

    @Test
    fun `organizations fixture decodes`() = kotlinx.coroutines.runBlocking {
        val orgs = api("organizations.json").organizations()
        assertEquals("Demo Org", orgs.single().name)
        assertEquals("1", orgs.single().sites.first().analyticsId)
    }

    @Test
    fun `org sites fixture decodes`() = kotlinx.coroutines.runBlocking {
        val result = api("org_sites.json").orgSites("4JhdU6D7rusbjxQQGwl7BjLRSUkilP5f")
        assertEquals("demo-org", result.organization?.slug)
        assertEquals("demo.example.com", result.sites.first().domain)
        assertEquals("1", result.sites.first().analyticsId)
    }

    @Test
    fun `site fixture decodes`() = kotlinx.coroutines.runBlocking {
        val site = api("site.json").site("1")
        assertEquals("Demo Site", site.name)
        assertEquals(1L, site.siteId)
        assertEquals("9bca2b450d80", site.id)
        assertEquals("1", site.analyticsId)
    }

    @Test
    fun `overview fixture decodes`() = kotlinx.coroutines.runBlocking {
        val overview = api("overview.json").overview("1", query)
        assertEquals(16, overview.sessions)
        assertEquals(103, overview.pageviews)
        assertEquals(6.4375, overview.pagesPerSession!!, 0.0001)
    }

    @Test
    fun `overview filtered fixture decodes`() = kotlinx.coroutines.runBlocking {
        val overview = api("overview_filtered.json").overview("1", query)
        assertEquals(4, overview.sessions)
        assertEquals(27, overview.pageviews)
    }

    @Test
    fun `overview time series day fixture decodes`() = kotlinx.coroutines.runBlocking {
        val points = api("overview_timeseries_day.json").overviewTimeSeries("1", query, TimeBucket.Day)
        assertEquals("2026-11-01 00:00:00", points.first().time)
        assertTrue(points.size > 1)
    }

    @Test
    fun `overview time series hour fixture decodes`() = kotlinx.coroutines.runBlocking {
        val points = api("overview_timeseries_hour.json").overviewTimeSeries("1", query, TimeBucket.Hour)
        assertEquals("2026-11-15 09:00:00", points.first().time)
    }

    @Test
    fun `every metric fixture decodes into a page`() = kotlinx.coroutines.runBlocking {
        val metricFixtures = listOf(
            "metric_browser.json" to "browser",
            "metric_channel.json" to "channel",
            "metric_country.json" to "country",
            "metric_device_type.json" to "device_type",
            "metric_operating_system.json" to "operating_system",
            "metric_pathname.json" to "pathname",
            "metric_referrer.json" to "referrer",
        )
        metricFixtures.forEach { (file, parameter) ->
            val page = api(file).metric("1", query, parameter)
            assertTrue("$file has rows", page.data.isNotEmpty())
            assertNotNull("$file value", page.data.first().value)
        }
    }

    @Test
    fun `browser metric values and totals decode`() = kotlinx.coroutines.runBlocking {
        val page = api("metric_browser.json").metric("1", query, "browser")
        assertEquals("Firefox", page.data.first().value)
        assertEquals(4, page.data.first().count)
        assertEquals(25.0, page.data.first().percentage!!, 0.0001)
    }

    @Test
    fun `device metric total count decodes`() = kotlinx.coroutines.runBlocking {
        val page = api("metric_device_type.json").metric("1", query, "device_type")
        assertEquals(2, page.totalCount)
    }

    @Test
    fun `pathname metric time on page decodes`() = kotlinx.coroutines.runBlocking {
        val first = api("metric_pathname.json").metric("1", query, "pathname").data.first()
        assertEquals("/pricing", first.value)
        assertEquals(0.058823529411764705, first.timeOnPageSeconds!!, 1e-9)
    }

    @Test
    fun `live user count fixture decodes`() = kotlinx.coroutines.runBlocking {
        assertEquals(16, api("live_user_count.json").liveUserCount("1").count)
    }

    @Test
    fun `sessions fixture decodes with string and numeric versions`() = kotlinx.coroutines.runBlocking {
        val sessions = api("sessions.json").sessions("1", query)
        val first = sessions.first()
        assertEquals("iAyU8X03pz02UR", first.sessionId)
        assertEquals("Safari", first.browser)
        assertEquals("17", first.browserVersion)
        assertEquals("14.5", first.operatingSystemVersion)
        assertTrue(sessions.all { it.sessionId.isNotBlank() })
    }

    @Test
    fun `session detail fixture decodes events and pagination`() = kotlinx.coroutines.runBlocking {
        val detail = api("session_detail.json").sessionDetail("1", "iAyU8X03pz02UR")
        assertEquals("iAyU8X03pz02UR", detail.session?.sessionId)
        assertEquals("/docs/getting-started", detail.events.first().pathname)
        assertEquals("pageview", detail.events.first().type)
        assertEquals(3, detail.pagination?.total)
        assertEquals(false, detail.pagination?.hasMore)
    }

    @Test
    fun `session locations fixture decodes`() = kotlinx.coroutines.runBlocking {
        val locations = api("session_locations.json").sessionLocations("1", query)
        assertEquals("JP", locations.first().country)
        assertEquals(3, locations.first().count)
    }

    @Test
    fun `events fixture decodes with cursor and string properties`() = kotlinx.coroutines.runBlocking {
        val response = api("events.json").events("1", query)
        assertTrue(response.cursor?.hasMore == true)
        assertEquals("2026-11-16 08:41:48", response.cursor?.oldestTimestamp)
        assertEquals("/signup", response.data.first().pathname)
        assertEquals("{}", response.data.first().properties)
    }

    @Test
    fun `event names fixture decodes`() = kotlinx.coroutines.runBlocking {
        val names = api("event_names.json").eventNames("1", query)
        assertEquals("signup_click", names.single().eventName)
        assertEquals(28, names.single().count)
    }

    @Test
    fun `event properties fixture decodes`() = kotlinx.coroutines.runBlocking {
        val props = api("event_properties.json").eventProperties("1", query, "signup_click")
        assertEquals("plan", props.first().propertyKey)
        assertEquals("pro", props.first().propertyValue)
    }

    @Test
    fun `events time series fixture decodes`() = kotlinx.coroutines.runBlocking {
        val points = api("events_timeseries.json").eventsTimeSeries("1", query, TimeBucket.Day)
        assertEquals("signup_click", points.single().eventName)
        assertEquals(28, points.single().eventCount)
    }

    @Test
    fun `error names fixture decodes`() = kotlinx.coroutines.runBlocking {
        val names = api("errors_names.json").errorNames("1", query)
        assertEquals("TypeError", names.single().errorName)
        assertEquals(12, names.single().count)
    }

    @Test
    fun `error events fixture decodes`() = kotlinx.coroutines.runBlocking {
        val events = api("errors_events.json").errorEvents("1", query, "Cannot read properties of undefined (reading 'map')")
        assertTrue(events.isNotEmpty())
        assertTrue(events.first().message!!.startsWith("Cannot read"))
    }

    @Test
    fun `error time series fixture decodes wrapped with success flag`() = kotlinx.coroutines.runBlocking {
        val points = api("errors_timeseries.json").errorTimeSeries("1", query, TimeBucket.Day, errorMessage = "Cannot read properties")
        assertEquals("2026-11-01 00:00:00", points.first().time)
        assertEquals(0, points.first().errorCount)
    }

    @Test
    fun `goals fixture decodes with string meta total`() = kotlinx.coroutines.runBlocking {
        val response = api("goals.json").goals("1", query)
        assertEquals(2, response.meta?.total)
        assertEquals(2, response.data.first().goalId)
        assertEquals(0.625, response.data.first().conversionRate!!, 1e-9)
    }

    @Test
    fun `goal sessions fixture decodes`() = kotlinx.coroutines.runBlocking {
        val sessions = api("goal_sessions.json").goalSessions("1", query, 2)
        assertEquals("cP4TnTwLCpXHcq", sessions.first().sessionId)
    }

    @Test
    fun `funnels fixture decodes`() = kotlinx.coroutines.runBlocking {
        val funnel = api("funnels.json").funnels("1").single()
        assertEquals("Signup funnel", funnel.name)
        assertEquals(3, funnel.steps.size)
        assertEquals("/pricing", funnel.steps[1].value)
    }

    @Test
    fun `funnel analyze fixture decodes`() = kotlinx.coroutines.runBlocking {
        val steps = api("funnel_analyze.json").analyzeFunnel("1", query, emptyList())
        assertEquals(3, steps.size)
        assertEquals(25.0, steps.last().conversionRate!!, 1e-9)
        assertEquals(71.43, steps.last().dropoffRate!!, 1e-9)
    }

    @Test
    fun `users fixture decodes with top level paging`() = kotlinx.coroutines.runBlocking {
        val response = api("users.json").users("1", query)
        assertEquals(16, response.totalCount)
        assertEquals("6054d69d315c", response.data.first().userId)
    }

    @Test
    fun `user detail fixture decodes locations and devices`() = kotlinx.coroutines.runBlocking {
        val detail = api("user_detail.json").userDetail("1", "db8f8c98c762")
        assertEquals("America/Chicago", detail.timezone)
        assertEquals(1, detail.locations.size)
        assertEquals("Safari", detail.devices.first().browser)
    }

    @Test
    fun `retention fixture decodes cohorts map`() = kotlinx.coroutines.runBlocking {
        val retention = api("retention.json").retention("1")
        assertEquals(16, retention.cohorts["2026-11-16"]?.size)
        assertEquals(listOf(100.0), retention.cohorts["2026-11-16"]?.percentages)
    }

    @Test
    fun `performance overview fixture decodes nulls`() = kotlinx.coroutines.runBlocking {
        val perf = api("performance_overview.json").performanceOverview("1", query)
        assertEquals(null, perf.lcpP50)
        assertEquals(0, perf.totalPerformanceEvents)
    }
}
