package app.rybbitclient.core.network

import app.rybbitclient.testsupport.FixtureFiles
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards against silently skipped fixtures. Every file in docs/fixtures must be claimed by a decoding
 * test (FixtureDecodingTest) or an error-mapping test (ApiErrorMappingTest), and must be valid JSON.
 */
class FixtureInventoryTest {

    private val coveredByDecodingTests = setOf(
        "errors_events.json", "errors_names.json", "errors_timeseries.json", "event_names.json",
        "event_properties.json", "events.json", "events_timeseries.json", "funnel_analyze.json",
        "funnels.json", "goal_sessions.json", "goals.json", "live_user_count.json",
        "metric_browser.json", "metric_channel.json", "metric_country.json", "metric_device_type.json",
        "metric_operating_system.json", "metric_pathname.json", "metric_referrer.json",
        "org_sites.json", "organizations.json", "overview.json", "overview_filtered.json",
        "overview_timeseries_day.json", "overview_timeseries_hour.json", "performance_overview.json",
        "retention.json", "session_detail.json", "session_locations.json", "sessions.json", "site.json",
        "user_detail.json", "users.json",
    )

    private val coveredByErrorTests = setOf("not_found_site.json", "pages.json", "unauthorized.json")

    private val coveredByDirectDecode = setOf("segments.json")

    private fun fixtureNames(): Set<String> {
        val directory = listOf("../docs/fixtures", "docs/fixtures").map(::File).first { it.isDirectory }
        return directory.listFiles().orEmpty().filter { it.name.endsWith(".json") }.map { it.name }.toSet()
    }

    @Test
    fun `every fixture file is claimed by a test`() {
        val claimed = coveredByDecodingTests + coveredByErrorTests + coveredByDirectDecode
        assertEquals("Unclaimed fixtures", emptySet<String>(), fixtureNames() - claimed)
        assertEquals("Tests reference missing fixtures", emptySet<String>(), claimed - fixtureNames())
    }

    @Test
    fun `every fixture parses as json`() {
        fixtureNames().forEach { name ->
            val element = RybbitJson.json.parseToJsonElement(FixtureFiles.read(name))
            assertTrue("$name is not an object or array", element is JsonObject || element is JsonArray)
        }
    }

    @Test
    fun `segments fixture is a bare array`() {
        val element = RybbitJson.json.parseToJsonElement(FixtureFiles.read("segments.json"))
        assertTrue(element is JsonArray)
    }
}
