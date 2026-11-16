package app.rybbitclient.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UrlNormalizerTest {

    private fun normalized(input: String): String = UrlNormalizer.normalize(input).baseUrl

    @Test
    fun `bare host gets https scheme`() {
        assertEquals("https://analytics.example.com", normalized("analytics.example.com"))
    }

    @Test
    fun `trailing slash is removed`() {
        assertEquals("https://analytics.example.com", normalized("https://analytics.example.com/"))
    }

    @Test
    fun `api suffix is dropped`() {
        assertEquals("https://x.com", normalized("https://x.com/api"))
        assertEquals("https://x.com", normalized("https://x.com/api/"))
    }

    @Test
    fun `reverse proxy sub path is kept`() {
        assertEquals("https://example.com/rybbit", normalized("https://example.com/rybbit/"))
        assertEquals("https://example.com/rybbit", normalized("example.com/rybbit/api"))
    }

    @Test
    fun `explicit port is kept`() {
        assertEquals("https://analytics.example.com:8443", normalized("analytics.example.com:8443"))
    }

    @Test
    fun `ip address with port over http is kept`() {
        val result = UrlNormalizer.normalize("http://192.168.1.10:3001/")
        assertEquals("http://192.168.1.10:3001", result.baseUrl)
        assertTrue(result.isInsecure)
    }

    @Test
    fun `bare ip with port defaults to https`() {
        assertEquals("https://192.168.1.10:3001", normalized("192.168.1.10:3001"))
    }

    @Test
    fun `https is not insecure`() {
        assertFalse(UrlNormalizer.normalize("https://app.rybbit.io").isInsecure)
    }

    @Test
    fun `scheme and host are lowercased`() {
        assertEquals("https://analytics.example.com", normalized("HTTPS://Analytics.Example.COM"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("https://analytics.example.com", normalized("   analytics.example.com \n"))
    }

    @Test
    fun `cloud preset normalizes to app rybbit io`() {
        assertEquals(UrlNormalizer.CLOUD_BASE_URL, normalized("app.rybbit.io/"))
        assertEquals("https://app.rybbit.io", UrlNormalizer.CLOUD_BASE_URL)
    }

    @Test
    fun `multiple trailing slashes are removed`() {
        assertEquals("https://x.com/a", normalized("https://x.com/a///"))
    }

    @Test
    fun `ipv6 literal is accepted`() {
        assertEquals("http://[::1]:3001", normalized("http://[::1]:3001"))
    }

    @Test
    fun `empty input is rejected`() {
        assertInvalid("")
        assertInvalid("   ")
    }

    @Test
    fun `unsupported scheme is rejected`() {
        assertInvalid("ftp://analytics.example.com")
    }

    @Test
    fun `input with spaces inside host is rejected`() {
        assertInvalid("analytics example.com")
    }

    @Test
    fun `missing host is rejected`() {
        assertInvalid("https://")
        assertInvalid("https:///api")
    }

    @Test
    fun `query and fragment are rejected`() {
        assertInvalid("https://x.com/?a=1")
        assertInvalid("https://x.com/#top")
    }

    @Test
    fun `user info is rejected`() {
        assertInvalid("https://user:pass@x.com")
    }

    @Test
    fun `non numeric port is rejected`() {
        assertInvalid("https://x.com:abc")
    }

    private fun assertInvalid(input: String) {
        try {
            UrlNormalizer.normalize(input)
            fail("Expected InvalidUrlException for '$input'")
        } catch (expected: InvalidUrlException) {
            // expected
        }
    }

    @Test
    fun `cloud host is refused over plain http`() {
        val failure = runCatching { UrlNormalizer.normalize("http://app.rybbit.io") }.exceptionOrNull()
        assertTrue(failure is InvalidUrlException)
    }

    @Test
    fun `self-hosted host may still use plain http`() {
        assertEquals("http://192.168.1.10:3001", UrlNormalizer.normalize("http://192.168.1.10:3001").baseUrl)
    }
}
