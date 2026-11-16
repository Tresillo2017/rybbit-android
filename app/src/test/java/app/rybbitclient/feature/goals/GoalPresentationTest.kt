package app.rybbitclient.feature.goals

import app.rybbitclient.core.model.Funnel
import app.rybbitclient.core.model.FunnelStep
import app.rybbitclient.core.model.Goal
import app.rybbitclient.core.model.PageMeta
import app.rybbitclient.core.model.FunnelStepResult
import app.rybbitclient.core.ui.components.FunnelStepItem
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalPresentationTest {

    @Test
    fun `path goals read the page pattern and event goals read the event name`() {
        // Arrange
        val path = goal(type = "path", config = "pathPattern" to "/pricing")
        val event = goal(type = "event", config = "eventName" to "signup_click")

        // Act and Assert
        assertEquals(GoalKind.Path, goalKindOf(path.goalType))
        assertEquals("/pricing", goalTargetOf(path))
        assertEquals(GoalKind.Event, goalKindOf(event.goalType))
        assertEquals("signup_click", goalTargetOf(event))
    }

    @Test
    fun `an unknown goal type has no target and falls back to a generic badge`() {
        // Arrange
        val other = goal(type = "scroll", config = "pathPattern" to "/x")

        // Act and Assert
        assertEquals(GoalKind.Other, goalKindOf(other.goalType))
        assertNull(goalTargetOf(other))
    }

    @Test
    fun `a missing config yields no target`() {
        // Arrange
        val goal = Goal(goalId = 1, name = "Bare", goalType = "event", config = null)

        // Act and Assert
        assertNull(goalTargetOf(goal))
    }

    @Test
    fun `the server conversion fraction is used and clamped to the unit range`() {
        // Arrange
        val half = Goal(goalId = 1, name = "Half", conversionRate = 0.5)
        val overflow = Goal(goalId = 2, name = "Overflow", conversionRate = 1.4)

        // Act and Assert
        assertEquals(0.5, goalConversionFraction(half)!!, 0.0001)
        assertEquals(1.0, goalConversionFraction(overflow)!!, 0.0001)
    }

    @Test
    fun `conversion falls back to totals and is null without them`() {
        // Arrange
        val fromTotals = Goal(goalId = 1, name = "Totals", totalConversions = 10, totalSessions = 16)
        val noSessions = Goal(goalId = 2, name = "No sessions", totalConversions = 3, totalSessions = 0)
        val empty = Goal(goalId = 3, name = "Empty")

        // Act and Assert
        assertEquals(0.625, goalConversionFraction(fromTotals)!!, 0.0001)
        assertNull(goalConversionFraction(noSessions))
        assertNull(goalConversionFraction(empty))
    }

    @Test
    fun `more pages follow the server total pages when present`() {
        // Act and Assert
        assertTrue(hasMorePages(PageMeta(totalPages = 3), page = 2, pageSize = 10, received = 10))
        assertFalse(hasMorePages(PageMeta(totalPages = 3), page = 3, pageSize = 10, received = 10))
    }

    @Test
    fun `more pages are derived from a string total the core decoded`() {
        // Act and Assert: 25 goals at 10 per page is 3 pages
        assertTrue(hasMorePages(PageMeta(total = 25), page = 2, pageSize = 10, received = 10))
        assertFalse(hasMorePages(PageMeta(total = 20), page = 2, pageSize = 10, received = 10))
    }

    @Test
    fun `without metadata a full page means there may be more`() {
        // Act and Assert
        assertTrue(hasMorePages(meta = null, page = 1, pageSize = 10, received = 10))
        assertFalse(hasMorePages(meta = null, page = 1, pageSize = 10, received = 4))
    }

    @Test
    fun `analysis maps step rows to chart steps and the overall conversion`() {
        // Arrange: shape of docs/fixtures/funnel_analyze.json
        val results = listOf(
            FunnelStepResult(stepNumber = 1, stepName = "Home", sessions = 8, conversionRate = 100.0, dropoffRate = 0.0, visitors = 8),
            FunnelStepResult(stepNumber = 2, stepName = "Pricing", sessions = 7, conversionRate = 87.5, dropoffRate = 12.5, visitors = 7),
            FunnelStepResult(stepNumber = 3, stepName = "Signup", sessions = 2, conversionRate = 25.0, dropoffRate = 71.43, visitors = 2),
        )

        // Act
        val analysis = analyzeFunnelResults(results)

        // Assert
        assertEquals(
            listOf(
                FunnelStepItem("Home", 8.0, 100.0, 0.0),
                FunnelStepItem("Pricing", 7.0, 87.5, 12.5),
                FunnelStepItem("Signup", 2.0, 25.0, 71.43),
            ),
            analysis.steps,
        )
        assertEquals(8, analysis.enteredVisitors)
        assertEquals(2, analysis.completedVisitors)
        assertEquals(25.0, analysis.overallConversionPercent!!, 0.0001)
    }

    @Test
    fun `analysis without results is empty and has no conversion`() {
        // Act
        val analysis = analyzeFunnelResults(emptyList())

        // Assert
        assertTrue(analysis.steps.isEmpty())
        assertNull(analysis.enteredVisitors)
        assertNull(analysis.overallConversionPercent)
    }

    @Test
    fun `a step with no name is labelled by its number`() {
        // Arrange
        val results = listOf(FunnelStepResult(stepNumber = null, stepName = null, visitors = 4, conversionRate = 100.0))

        // Act and Assert
        assertEquals("Step 1", analyzeFunnelResults(results).steps.single().label)
    }

    @Test
    fun `step preview shows up to three steps and an ellipsis for more`() {
        // Arrange
        val short = funnel(listOf("Home", "Pricing"))
        val long = funnel(listOf("Home", "Pricing", "Signup", "Checkout"))

        // Act and Assert
        assertEquals("Home → Pricing", funnelStepPreview(short))
        assertEquals("Home → Pricing → Signup → …", funnelStepPreview(long))
    }

    private fun goal(type: String, config: Pair<String, String>) = Goal(
        goalId = 1,
        name = "Goal",
        goalType = type,
        config = JsonObject(mapOf(config.first to JsonPrimitive(config.second))),
    )

    private fun funnel(names: List<String>) = Funnel(
        id = 1,
        name = "Funnel",
        steps = names.map { FunnelStep(type = "page", value = "/$it", name = it) },
    )
}
