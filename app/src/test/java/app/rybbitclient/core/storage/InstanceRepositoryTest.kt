package app.rybbitclient.core.storage

import app.rybbitclient.core.model.AuthType
import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.model.InstanceKind
import app.rybbitclient.core.network.Credential
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class InstanceRepositoryTest {

    private val preferences = InMemoryKeyValueStore()
    private val secrets = InMemorySecretStore()
    private val repository = InstanceRepository(preferences, secrets)

    private fun instance(id: String, name: String = "Server $id") = Instance(
        id = id,
        displayName = name,
        baseUrl = "https://$id.example.com",
        kind = InstanceKind.SelfHosted,
        authType = AuthType.ApiKey,
        accountLabel = null,
        createdAtMillis = 1_000L,
    )

    @Test
    fun `starts with no instances and no active instance`() = runBlocking {
        assertEquals(emptyList<Instance>(), repository.instances.first())
        assertNull(repository.activeInstanceId.first())
        assertNull(repository.activeInstance.first())
    }

    @Test
    fun `first added instance becomes active and later ones do not`() = runBlocking {
        repository.add(instance("a"), Credential.ApiKey("key-a"))
        repository.add(instance("b"), Credential.ApiKey("key-b"))

        assertEquals(listOf("a", "b"), repository.instances.first().map { it.id })
        assertEquals("a", repository.activeInstanceId.first())
        assertEquals("a", repository.activeInstance.first()?.id)
    }

    @Test
    fun `credential is stored only in the secret store, never in preferences`() = runBlocking {
        repository.add(instance("a"), Credential.ApiKey("very-secret-key"))

        assertEquals(Credential.ApiKey("very-secret-key"), repository.credentialFor("a"))
        val rawInstances = preferences.read("instances").orEmpty()
        assertFalse(rawInstances.contains("very-secret-key"))
        assertTrue(rawInstances.contains("https://a.example.com"))
    }

    @Test
    fun `adding a duplicate id is rejected`() = runBlocking {
        repository.add(instance("a"), Credential.ApiKey("k"))
        expectIllegalArgument { repository.add(instance("a"), Credential.ApiKey("k2")) }
    }

    @Test
    fun `update changes metadata but keeps the credential`() = runBlocking {
        repository.add(instance("a"), Credential.ApiKey("k"))
        repository.update(instance("a", name = "Renamed"))

        assertEquals("Renamed", repository.instance("a")?.displayName)
        assertEquals(Credential.ApiKey("k"), repository.credentialFor("a"))
    }

    @Test
    fun `update of an unknown instance is rejected`() = runBlocking {
        expectIllegalArgument { repository.update(instance("ghost")) }
    }

    @Test
    fun `updateCredential replaces the stored secret`() = runBlocking {
        repository.add(instance("a"), Credential.Session("better-auth.session_token", "old"))
        repository.updateCredential("a", Credential.ApiKey("fresh"))
        assertEquals(Credential.ApiKey("fresh"), repository.credentialFor("a"))
    }

    @Test
    fun `setActive switches the active instance and rejects unknown ids`() = runBlocking {
        repository.add(instance("a"), Credential.ApiKey("k"))
        repository.add(instance("b"), Credential.ApiKey("k"))

        repository.setActive("b")
        assertEquals("b", repository.activeInstanceId.first())
        expectIllegalArgument { repository.setActive("missing") }
    }

    @Test
    fun `removing an instance deletes its credential and metadata`() = runBlocking {
        repository.add(instance("a"), Credential.ApiKey("k"))
        repository.remove("a")

        assertEquals(emptyList<Instance>(), repository.instances.first())
        assertNull(repository.credentialFor("a"))
        assertNull(repository.activeInstanceId.first())
    }

    @Test
    fun `removing the active instance activates the first remaining one`() = runBlocking {
        repository.add(instance("a"), Credential.ApiKey("k"))
        repository.add(instance("b"), Credential.ApiKey("k"))
        repository.add(instance("c"), Credential.ApiKey("k"))
        repository.remove("a")

        assertEquals(listOf("b", "c"), repository.instances.first().map { it.id })
        assertEquals("b", repository.activeInstanceId.first())
    }

    @Test
    fun `removing a non active instance leaves the active one alone`() = runBlocking {
        repository.add(instance("a"), Credential.ApiKey("k"))
        repository.add(instance("b"), Credential.ApiKey("k"))
        repository.remove("b")
        assertEquals("a", repository.activeInstanceId.first())
    }

    @Test
    fun `removing an unknown id is a no-op`() = runBlocking {
        repository.add(instance("a"), Credential.ApiKey("k"))
        repository.remove("nope")
        assertEquals(listOf("a"), repository.instances.first().map { it.id })
    }

    @Test
    fun `last selected site is remembered per instance and cleared on removal`() = runBlocking {
        repository.add(instance("a"), Credential.ApiKey("k"))
        repository.add(instance("b"), Credential.ApiKey("k"))
        repository.setLastSelectedSiteId("a", "42")
        repository.setLastSelectedSiteId("b", "7")

        assertEquals("42", repository.lastSelectedSiteId("a").first())
        assertEquals("7", repository.lastSelectedSiteId("b").first())

        repository.remove("a")
        assertNull(repository.lastSelectedSiteId("a").first())
    }

    @Test
    fun `missing credential reads as null`() = runBlocking {
        assertNull(repository.credentialFor("never-added"))
    }

    @Test
    fun `insecure http instances are flagged`() {
        assertTrue(instance("a").copy(baseUrl = "http://192.168.1.10:3001").isInsecure)
        assertFalse(instance("a").isInsecure)
    }

    private suspend fun expectIllegalArgument(block: suspend () -> Unit) {
        try {
            block()
        } catch (expected: IllegalArgumentException) {
            return
        }
        fail("Expected IllegalArgumentException")
    }
}
