package app.rybbitclient.core.network

import app.rybbitclient.core.model.FunnelStep
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.query.FilterType
import app.rybbitclient.core.query.TimeBucket
import app.rybbitclient.testsupport.apiReplying
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** Verifies the wire shape of requests: paths, parameters, filters and bodies. Stub bodies may not decode. */
class RybbitApiRequestTest {

    private val zone = ZoneId.of("Europe/Madrid")

    /** Runs [block] against a stub server and returns the last request sent. Decode failures are expected here. */
    private fun capture(block: suspend RybbitApi.() -> Unit): HttpRequestData = runBlocking {
        val captured = mutableListOf<HttpRequestData>()
        val api = apiReplying("{}", captured = captured)
        runCatching { block(api) }
        captured.last()
    }

    private fun parameter(request: HttpRequestData, key: String): String? = request.url.parameters[key]

    @Test
    fun `overview sends the date window, zone and filters`() {
        val query = AnalyticsQuery(
            range = DateRange.Last7Days,
            filters = listOf(Filter("browser", FilterType.Equals, listOf("Firefox"))),
            zone = zone,
        )
        val request = capture { overview("1", query) }
        assertEquals("/api/sites/1/overview", request.url.encodedPath)
        assertEquals("Europe/Madrid", parameter(request, "time_zone"))
        assertEquals("""[{"parameter":"browser","type":"equals","value":["Firefox"]}]""", parameter(request, "filters"))
        assertTrue(parameter(request, "start_date") != null)
    }

    @Test
    fun `time series sends bucket value`() {
        val request = capture { overviewTimeSeries("1", AnalyticsQuery(DateRange.AllTime, zone = zone), TimeBucket.Week) }
        assertEquals("week", parameter(request, "bucket"))
    }

    @Test
    fun `metric sends parameter limit and page`() {
        val request = capture { metric("9", AnalyticsQuery(DateRange.AllTime), "pathname", limit = 25, page = 3) }
        assertEquals("pathname", parameter(request, "parameter"))
        assertEquals("25", parameter(request, "limit"))
        assertEquals("3", parameter(request, "page"))
    }

    @Test
    fun `events send page size and before cursor`() {
        val request = capture { events("1", AnalyticsQuery(DateRange.AllTime), pageSize = 50, beforeCursor = "1706_abc") }
        assertEquals("50", parameter(request, "page_size"))
        assertEquals("1706_abc", parameter(request, "before_cursor"))
    }

    @Test
    fun `error message is sent in camel case`() {
        val request = capture { errorEvents("1", AnalyticsQuery(DateRange.AllTime), "Cannot read 'map'", limit = 10) }
        assertEquals("Cannot read 'map'", parameter(request, "errorMessage"))
    }

    @Test
    fun `funnel analysis posts steps as json`() {
        val request = capture {
            analyzeFunnel("1", AnalyticsQuery(DateRange.AllTime), listOf(FunnelStep("page", "/", "Home"), FunnelStep("page", "/signup")))
        }
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(
            """{"steps":[{"type":"page","value":"/","name":"Home"},{"type":"page","value":"/signup"}]}""",
            (request.body as TextContent).text,
        )
    }

    @Test
    fun `api key creation posts the name as json`() {
        val request = capture { createApiKey("Rybbit Android (Pixel 8)") }
        assertEquals("/api/user/api-keys", request.url.encodedPath)
        assertEquals("""{"name":"Rybbit Android (Pixel 8)"}""", (request.body as TextContent).text)
    }

    @Test
    fun `retention sends mode range and zone`() {
        val request = capture { retention("1", mode = "week", range = 12, timeZone = "Europe/Madrid") }
        assertEquals("week", parameter(request, "mode"))
        assertEquals("12", parameter(request, "range"))
        assertEquals("Europe/Madrid", parameter(request, "time_zone"))
    }

    @Test
    fun `event properties name the event with the event_name parameter the server expects`() {
        val request = capture { eventProperties("1", AnalyticsQuery(DateRange.AllTime, zone = zone), "signup") }
        assertEquals("/api/sites/1/events/properties", request.url.encodedPath)
        assertEquals("signup", parameter(request, "event_name"))
        assertEquals(null, parameter(request, "eventName"))
    }

    @Test
    fun `events send the legacy before_timestamp bound when given`() {
        val request = capture {
            events("1", AnalyticsQuery(DateRange.AllTime, zone = zone), beforeTimestamp = "2026-11-16 10:00:00")
        }
        assertEquals("2026-11-16 10:00:00", parameter(request, "before_timestamp"))
        assertEquals(null, parameter(request, "before_cursor"))
    }

    @Test
    fun `live user count sends minutes`() {
        val request = capture { liveUserCount("1", minutes = 30) }
        assertEquals("30", parameter(request, "minutes"))
        assertEquals("/api/sites/1/live-user-count", request.url.encodedPath)
    }

    @Test
    fun `site id is percent encoded in the path`() {
        val request = capture { site("a/b c") }
        assertEquals("/api/sites/a%2Fb%20c", request.url.encodedPath)
    }

    @Test
    fun `base url sub path is preserved`() {
        val captured = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            captured += request
            respond(content = "[]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = RybbitApi(HttpClientFactory("RybbitAndroid/test", engine).create(), "https://example.com/rybbit")
        runBlocking { api.organizations() }
        assertEquals("/rybbit/api/organizations", captured.single().url.encodedPath)
    }

    @Test
    fun `non success statuses are reported through the mapper not decoded`() {
        val api = apiReplying("{}", HttpStatusCode.TooManyRequests, retryAfter = "3")
        val failure = runCatching { runBlocking { api.organizations() } }.exceptionOrNull()
        assertTrue(failure is RateLimited)
    }
}
