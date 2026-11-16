package app.rybbitclient.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolvedBaseUrlTest {

    @Test
    fun noRedirectKeepsRequestedBase() =
        assertEquals("https://a.com", resolvedBaseUrl("https://a.com", "https://a.com/api/health"))

    @Test
    fun schemeUpgradeIsAdopted() =
        assertEquals("https://a.com", resolvedBaseUrl("http://a.com", "https://a.com/api/health"))

    @Test
    fun subPathTargetIsKept() =
        assertEquals("https://a.com/rybbit", resolvedBaseUrl("https://a.com", "https://a.com/rybbit/api/health"))

    @Test
    fun queryStringIsIgnored() =
        assertEquals("https://b.com", resolvedBaseUrl("https://a.com", "https://b.com/api/health?x=1"))

    @Test
    fun nonHealthTargetFallsBackToRequested() =
        assertEquals("https://a.com", resolvedBaseUrl("https://a.com", "https://login.a.com/sso"))
}
