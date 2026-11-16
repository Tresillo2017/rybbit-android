package app.rybbitclient.feature.performance

import app.rybbitclient.core.model.PerformanceOverview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class VitalsModelsTest {

    @Test
    fun `lcp boundaries sit at the published good and poor limits`() {
        // Act and Assert
        assertEquals(VitalRating.Good, VitalMetric.Lcp.rate(2500.0))
        assertEquals(VitalRating.NeedsImprovement, VitalMetric.Lcp.rate(2500.1))
        assertEquals(VitalRating.NeedsImprovement, VitalMetric.Lcp.rate(4000.0))
        assertEquals(VitalRating.Poor, VitalMetric.Lcp.rate(4000.1))
    }

    @Test
    fun `cls uses unitless thresholds of 0_1 and 0_25`() {
        // Act and Assert
        assertEquals(VitalRating.Good, VitalMetric.Cls.rate(0.1))
        assertEquals(VitalRating.NeedsImprovement, VitalMetric.Cls.rate(0.2))
        assertEquals(VitalRating.Poor, VitalMetric.Cls.rate(0.26))
    }

    @Test
    fun `inp fcp and ttfb use their own thresholds`() {
        // Act and Assert
        assertEquals(VitalRating.Good, VitalMetric.Inp.rate(200.0))
        assertEquals(VitalRating.Poor, VitalMetric.Inp.rate(501.0))
        assertEquals(VitalRating.NeedsImprovement, VitalMetric.Fcp.rate(2000.0))
        assertEquals(VitalRating.Good, VitalMetric.Ttfb.rate(800.0))
        assertEquals(VitalRating.Poor, VitalMetric.Ttfb.rate(1801.0))
    }

    @Test
    fun `milliseconds under a second are whole ms and larger values are seconds`() {
        // Act and Assert
        assertEquals("870 ms", formatVitalValue(VitalMetric.Fcp, 870.4, Locale.US))
        assertEquals("2.50 s", formatVitalValue(VitalMetric.Lcp, 2500.0, Locale.US))
    }

    @Test
    fun `cls is shown with three decimals and missing values as a dash`() {
        // Act and Assert
        assertEquals("0.084", formatVitalValue(VitalMetric.Cls, 0.0841, Locale.US))
        assertEquals("—", formatVitalValue(VitalMetric.Cls, null, Locale.US))
    }

    @Test
    fun `readings follow display order and carry each percentile`() {
        // Arrange
        val overview = PerformanceOverview(lcpP50 = 1.0, lcpP75 = 2.0, lcpP90 = 3.0, lcpP99 = 4.0)

        // Act
        val readings = vitalReadings(overview)

        // Assert
        assertEquals(listOf("LCP", "INP", "CLS", "FCP", "TTFB"), readings.map { it.metric.shortName })
        val lcp = readings.first()
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), listOf(lcp.p50, lcp.p75, lcp.p90, lcp.p99))
    }

    @Test
    fun `an overview with no percentile at all has no vital values`() {
        // Act and Assert
        assertTrue(hasNoVitalValues(PerformanceOverview(totalPerformanceEvents = 0)))
        assertFalse(hasNoVitalValues(PerformanceOverview(ttfbP99 = 900.0)))
    }

    @Test
    fun `a reading with no p75 has no rating`() {
        // Arrange
        val reading = VitalReading(VitalMetric.Inp, p50 = 100.0, p75 = null, p90 = null, p99 = null)

        // Act and Assert
        assertNull(reading.rating)
    }
}
