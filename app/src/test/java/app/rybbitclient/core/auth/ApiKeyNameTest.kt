package app.rybbitclient.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyNameTest {

    @Test
    fun shortDeviceNameIsKeptVerbatim() {
        assertEquals("Rybbit Android (Pixel 8)", apiKeyName("Pixel 8"))
    }

    @Test
    fun longDeviceNameIsCappedToServerLimit() {
        val name = apiKeyName("SM-S928B Galaxy S24 Ultra Enterprise Edition")
        assertTrue(name.length <= MAX_API_KEY_NAME_LENGTH)
        assertTrue(name.startsWith("Rybbit Android (SM-S928B"))
        assertTrue(name.endsWith(")"))
    }

    @Test
    fun blankLabelFallsBack() {
        assertEquals("Rybbit Android (device)", apiKeyName("   "))
    }
}
