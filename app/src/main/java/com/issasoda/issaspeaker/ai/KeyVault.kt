package com.issasoda.issaspeaker.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.StoreBytesData
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.tasks.await
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts the owner's AI keys so only their own phones can read them (catalog F013, F423; design
 * docs/design/KEY-VAULT.md). Same construction as the Movie Recommendation app.
 *
 * - **Vault key:** 32 random bytes, generated on the first phone. Never in the APK, the repository or logs.
 * - **On this phone** it is wrapped by an Android Keystore key (StrongBox when the phone has it), in
 *   `secure_prefs.xml`, which cloud backup and device transfer leave out (data_extraction_rules.xml, F017).
 * - **Block Store** (Google Play services) keeps a copy with cloud backup on; Google encrypts it end to end with
 *   the screen lock. It comes back after a reinstall or on a new phone restored from the Google backup.
 * - **Ciphertexts** are AES-256-GCM under the vault key, bound to the provider id as associated data, so a
 *   ciphertext cannot be moved to another provider's entry.
 *
 * Residual risk, stated plainly: someone who knows the screen lock and controls the Google account can restore the
 * vault on their own phone, the same boundary as the passwords in Google Password Manager.
 */
class KeyVault(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val blockstore = Blockstore.getClient(context)

    /**
     * The vault key: from this phone, else from Block Store; a new one when [createIfMissing]. Null when neither has
     * it (a new phone set up without the Google backup): ciphertexts made elsewhere cannot be read here.
     */
    suspend fun vaultKey(createIfMissing: Boolean): ByteArray? {
        unwrapLocal()?.let { return it }
        restoreFromBlockstore()?.let { key ->
            wrapLocal(key)
            return key
        }
        if (!createIfMissing) return null
        val key = ByteArray(VAULT_KEY_BYTES).also { SecureRandom().nextBytes(it) }
        wrapLocal(key)
        backUpToBlockstore(key)
        return key
    }

    /** "Erase everything": the vault key leaves this phone and Block Store, so no ciphertext can be opened again. */
    suspend fun destroy() {
        prefs.edit { remove(KEY_WRAPPED) }
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEYSTORE_ALIAS)
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Keystore key not deleted", e)
        }
        try {
            blockstore.deleteBytes(DeleteBytesRequest.Builder().setKeys(listOf(BLOCKSTORE_KEY)).build()).await()
        } catch (e: ApiException) {
            Log.w(TAG, "Block Store copy not deleted", e)
        }
    }

    /** AES-256-GCM: base64 of IV followed by ciphertext and tag. */
    fun encrypt(vaultKey: ByteArray, plaintext: String, associatedData: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(vaultKey, "AES"))
        cipher.updateAAD(associatedData.toByteArray())
        val sealed = cipher.iv + cipher.doFinal(plaintext.toByteArray())
        return Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    /** Null when the ciphertext was not made with this vault key and associated data (or was altered). */
    fun decrypt(vaultKey: ByteArray, sealedBase64: String, associatedData: String): String? = try {
        val sealed = Base64.decode(sealedBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(vaultKey, "AES"), GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES))
        cipher.updateAAD(associatedData.toByteArray())
        String(cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES))
    } catch (e: GeneralSecurityException) {
        Log.w(TAG, "AI key ciphertext could not be opened with this vault key", e)
        null
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "AI key ciphertext is not valid base64", e)
        null
    }

    private fun wrapLocal(vaultKey: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val sealed = cipher.iv + cipher.doFinal(vaultKey)
        prefs.edit { putString(KEY_WRAPPED, Base64.encodeToString(sealed, Base64.NO_WRAP)) }
    }

    private fun unwrapLocal(): ByteArray? {
        val stored = prefs.getString(KEY_WRAPPED, null) ?: return null
        return try {
            val sealed = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES))
            cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
        } catch (e: GeneralSecurityException) {
            // The Keystore key is gone (app data cleared, lock screen removed): fall back to Block Store.
            Log.w(TAG, "Local vault key unreadable", e)
            prefs.edit { remove(KEY_WRAPPED) }
            null
        }
    }

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        fun generate(strongBox: Boolean): SecretKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEYSTORE_KEY_BITS)
                    .setIsStrongBoxBacked(strongBox)
                    .build(),
            )
            generateKey()
        }
        return try {
            generate(strongBox = true)
        } catch (_: StrongBoxUnavailableException) {
            generate(strongBox = false)
        }
    }

    private suspend fun restoreFromBlockstore(): ByteArray? = try {
        val request = RetrieveBytesRequest.Builder().setKeys(listOf(BLOCKSTORE_KEY)).build()
        blockstore.retrieveBytes(request).await().blockstoreDataMap[BLOCKSTORE_KEY]?.bytes
    } catch (e: ApiException) {
        Log.w(TAG, "Block Store unavailable", e)
        null
    }

    private suspend fun backUpToBlockstore(vaultKey: ByteArray) {
        try {
            val data = StoreBytesData.Builder().setKey(BLOCKSTORE_KEY).setBytes(vaultKey).setShouldBackupToCloud(true).build()
            blockstore.storeBytes(data).await()
        } catch (e: ApiException) {
            // The key still works on this phone; only the restore on another phone is lost.
            Log.w(TAG, "Vault key not backed up to Block Store", e)
        }
    }

    private companion object {
        const val TAG = "KeyVault"
        const val PREFS = "secure_prefs"
        const val KEY_WRAPPED = "wrapped_vault_key"
        const val BLOCKSTORE_KEY = "issaspeaker_ai_vault_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEYSTORE_ALIAS = "issaspeaker.ai_vault"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val VAULT_KEY_BYTES = 32
        const val KEYSTORE_KEY_BITS = 256
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
