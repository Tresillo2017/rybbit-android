package app.rybbitclient.core.storage

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Non-secret app state: instance list, active instance, last selected site per instance. */
val Context.rybbitPreferencesStore by preferencesDataStore(name = "rybbit_preferences")

/** Ciphertext of credentials. Values are AES/GCM encrypted by [KeystoreSecretCipher] before storage. */
val Context.rybbitSecretsStore by preferencesDataStore(name = "rybbit_secrets")
