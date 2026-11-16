package app.rybbitclient.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ResolvedBaseUrlTest {

    @Test
    fun noRedirectKeepsRequestedBase() =
        assertEquals("https://a.com", resolvedBaseUrl("https://a.com", "https://a.com/api/health"))

    @Test
    fun sameHostSchemeUpgradeIsAdopted() =
        assertEquals("https://a.com", resolvedBaseUrl("http://a.com", "https://a.com/api/health"))

    @Test
    fun sameHostSubPathIsAdopted() =
        assertEquals("https://a.com/rybbit", resolvedBaseUrl("https://a.com", "https://a.com/rybbit/api/health"))

    @Test
    fun queryStringIsIgnored() =
        assertEquals("https://a.com", resolvedBaseUrl("http://a.com", "https://a.com/api/health?x=1"))

    @Test
    fun nonHealthTargetFallsBackToRequested() =
        assertEquals("https://a.com", resolvedBaseUrl("https://a.com", "https://login.a.com/sso"))

    @Test
    fun otherHostIsRejected() {
        val error = assertThrows(UntrustedRedirect::class.java) {
            resolvedBaseUrl("http://a.com", "https://evil.example/api/health")
        }
        assertEquals("https://evil.example", error.target)
    }

    @Test
    fun httpsDowngradeIsRejected() {
        assertThrows(UntrustedRedirect::class.java) { resolvedBaseUrl("https://a.com", "http://a.com/api/health") }
    }

    @Test
    fun otherPortIsRejected() {
        assertThrows(UntrustedRedirect::class.java) {
            resolvedBaseUrl("http://localhost:3002", "http://localhost:3001/api/health")
        }
    }

    @Test
    fun userInfoIsRejected() {
        assertThrows(UntrustedRedirect::class.java) {
            resolvedBaseUrl("https://a.com", "https://user:pw@a.com/x/api/health")
        }
    }
}
