package app.rybbitclient.core.network

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

class LenientAndTimestampTest {

    @Serializable
    private data class Numbers(
        @Serializable(with = LenientIntSerializer::class) val int: Int? = null,
        @Serializable(with = LenientLongSerializer::class) val long: Long? = null,
        @Serializable(with = LenientDoubleSerializer::class) val double: Double? = null,
        @Serializable(with = FlexibleStringSerializer::class) val version: String? = null,
    )

    private fun decode(json: String): Numbers = RybbitJson.json.decodeFromString(Numbers.serializer(), json)

    @Test
    fun `int accepts number, numeric string and null`() {
        assertEquals(0, decode("""{"int":0}""").int)
        assertEquals(42, decode("""{"int":"42"}""").int)
        assertNull(decode("""{"int":null}""").int)
        assertNull(decode("{}").int)
    }

    @Test
    fun `int rejects non numeric text instead of guessing`() {
        try {
            decode("""{"int":"many"}""")
            fail("Expected a serialization failure")
        } catch (expected: kotlinx.serialization.SerializationException) {
            assertTrue(expected.message!!.contains("many"))
        }
    }

    @Test
    fun `long and double accept strings`() {
        val parsed = decode("""{"long":"9007199254740993","double":"0.5"}""")
        assertEquals(9007199254740993L, parsed.long)
        assertEquals(0.5, parsed.double!!, 1e-12)
    }

    @Test
    fun `double accepts integers`() {
        assertEquals(100.0, decode("""{"double":100}""").double!!, 1e-12)
    }

    @Test
    fun `version accepts number and string forms`() {
        assertEquals("133", decode("""{"version":133}""").version)
        assertEquals("14.5", decode("""{"version":"14.5"}""").version)
        assertEquals("", decode("""{"version":""}""").version)
        assertNull(decode("""{"version":null}""").version)
    }

    @Test
    fun `utc timestamps parse with 0 to 9 fraction digits`() {
        val expected = Instant.parse("2026-11-16T08:41:49Z")
        assertEquals(expected, RybbitTimestamps.parseUtc("2026-11-16 08:41:49"))
        assertEquals(expected.plusMillis(74), RybbitTimestamps.parseUtc("2026-11-16 08:41:49.074"))
        assertEquals(expected.plusNanos(323_700_000), RybbitTimestamps.parseUtc("2026-11-16 08:41:49.3237"))
        assertEquals(expected.plusNanos(383_811_000), RybbitTimestamps.parseUtc("2026-11-16 08:41:49.383811"))
        assertEquals(expected.plusNanos(1), RybbitTimestamps.parseUtc("2026-11-16 08:41:49.000000001"))
    }

    @Test
    fun `local bucket timestamps stay wall clock`() {
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0, 0), RybbitTimestamps.parseLocal("2026-11-01 00:00:00"))
    }

    @Test
    fun `timestamps reject ISO T separator`() {
        try {
            RybbitTimestamps.parseUtc("2026-11-16T08:41:49Z")
            fail("Expected parse failure")
        } catch (expected: java.time.format.DateTimeParseException) {
            assertTrue(expected.message != null)
        }
    }

    @Test
    fun `null safe parsers return null for garbage`() {
        assertNull(RybbitTimestamps.parseUtcOrNull("nope"))
        assertNull(RybbitTimestamps.parseLocalOrNull(null))
    }
}
