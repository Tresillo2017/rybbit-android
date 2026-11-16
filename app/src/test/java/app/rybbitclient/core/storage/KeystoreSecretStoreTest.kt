package app.rybbitclient.core.storage

import app.rybbitclient.core.network.Credential
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.security.GeneralSecurityException

class KeystoreSecretStoreTest {

    /** Reversible stand-in for the Keystore cipher, so the wrapper logic is testable on the JVM. */
    private class ReversingCipher : SecretCipher {
        override fun encrypt(plainText: String): String = "enc:" + plainText.reversed()

        override fun decrypt(encoded: String): String {
            if (!encoded.startsWith("enc:")) throw GeneralSecurityException("bad tag")
            return encoded.removePrefix("enc:").reversed()
        }
    }

    private val rawStore = InMemoryKeyValueStore()
    private val store = KeystoreSecretStore(ReversingCipher(), rawStore)

    @Test
    fun `api key round trips through encryption`() = runBlocking {
        store.save("a", Credential.ApiKey("rk_live_abc"))
        assertEquals(Credential.ApiKey("rk_live_abc"), store.load("a"))
    }

    @Test
    fun `session credential round trips with its cookie name`() = runBlocking {
        val session = Credential.Session("__Secure-better-auth.session_token", "tok.sig")
        store.save("a", session)
        assertEquals(session, store.load("a"))
    }

    @Test
    fun `stored value is ciphertext, not the plaintext secret`() = runBlocking {
        store.save("a", Credential.ApiKey("rk_live_abc"))
        val raw = rawStore.read("secret.a").orEmpty()
        assertFalse(raw.contains("rk_live_abc"))
    }

    @Test
    fun `undecryptable secret reads as null so the user signs in again`() = runBlocking {
        rawStore.write("secret.a", "garbage")
        assertNull(store.load("a"))
    }

    @Test
    fun `delete removes the secret`() = runBlocking {
        store.save("a", Credential.ApiKey("k"))
        store.delete("a")
        assertNull(store.load("a"))
    }

    @Test
    fun `in memory secret store behaves the same way`() = runBlocking {
        val memory = InMemorySecretStore()
        memory.save("a", Credential.ApiKey("k"))
        assertEquals(Credential.ApiKey("k"), memory.load("a"))
        memory.delete("a")
        assertNull(memory.load("a"))
    }
}
