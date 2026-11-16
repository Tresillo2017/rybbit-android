package app.rybbitclient.core.data

import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAvailabilityTest {

    @Test
    fun `features are available until marked missing`() {
        val features = FeatureAvailability()
        assertTrue(features.isAvailable("a", Feature.Pages))
    }

    @Test
    fun `marking is scoped to one instance`() {
        val features = FeatureAvailability()
        features.markUnavailable("cloud", Feature.Pages)
        assertFalse(features.isAvailable("cloud", Feature.Pages))
        assertTrue(features.isAvailable("self", Feature.Pages))
        assertTrue(features.isAvailable("cloud", Feature.Performance))
    }

    @Test
    fun `clear forgets an instance`() {
        val features = FeatureAvailability()
        features.markUnavailable("a", Feature.Pages)
        features.markUnavailable("a", Feature.Performance)
        features.clear("a")
        assertTrue(features.isAvailable("a", Feature.Pages))
        assertTrue(features.isAvailable("a", Feature.Performance))
    }

    @Test
    fun `observed set reflects marks`() {
        val features = FeatureAvailability()
        features.markUnavailable("a", Feature.Pages)
        features.markUnavailable("a", Feature.Pages)
        kotlinx.coroutines.runBlocking {
            assertEquals(setOf(Feature.Pages), features.observeUnavailable("a").first())
        }
    }
}
