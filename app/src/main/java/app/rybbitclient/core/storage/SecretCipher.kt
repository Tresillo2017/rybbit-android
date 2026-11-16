package app.rybbitclient.core.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts and decrypts secret strings. Implemented with the Android Keystore in production. */
interface SecretCipher {
    fun encrypt(plainText: String): String

    fun decrypt(encoded: String): String
}

/**
 * AES/GCM with a key held in the AndroidKeyStore (never exported). Output is `base64(iv).base64(ciphertext)`.
 * Decryption fails with a GeneralSecurityException if the key was lost or the data was tampered with.
 */
class KeystoreSecretCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) : SecretCipher {

    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = Base64.getEncoder().encodeToString(cipher.iv)
        val cipherText = Base64.getEncoder().encodeToString(cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)))
        return "$iv$SEPARATOR$cipherText"
    }

    override fun decrypt(encoded: String): String {
        val parts = encoded.split(SEPARATOR)
        require(parts.size == 2) { "Malformed secret payload" }
        val iv = Base64.getDecoder().decode(parts[0])
        val cipherText = Base64.getDecoder().decode(parts[1])
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "rybbit_credentials_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = "."
        const val GCM_TAG_BITS = 128
        const val KEY_SIZE_BITS = 256
    }
}
