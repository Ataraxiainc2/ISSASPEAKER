package com.issasoda.issaspeaker.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.security.GeneralSecurityException

/** A provider the owner added: what the screens may see. The key itself never leaves [AiKeyStore] unencrypted. */
data class SavedProvider(
    val providerId: String,
    val name: String,
    val endpoint: AiEndpoint,
    val modelId: String,
    val isDefault: Boolean,
)

/** The plain key for one provider, when this phone can open it. */
sealed interface KeyLookup {
    data class Ready(val provider: SavedProvider, val apiKey: String) : KeyLookup

    /** Saved, but this phone cannot open it (new phone without the Google backup): add the key again. */
    data class Unavailable(val provider: SavedProvider) : KeyLookup

    data object Missing : KeyLookup
}

@Serializable
private data class StoredProvider(
    val providerId: String,
    val name: String,
    val endpoint: AiEndpoint,
    val modelId: String,
    val keyCiphertext: String,
)

@Serializable
private data class StoredKeys(val defaultProviderId: String? = null, val providers: List<StoredProvider> = emptyList())

/**
 * The owner's AI providers and keys on this phone (catalog F051, F052), in `files/secrets/ai_keys.json`, which
 * cloud backup and device transfer leave out (F017). Each key is stored only as [KeyVault] ciphertext together
 * with the endpoint it was checked against, so a later change in the registry can never redirect it (F418).
 * Several providers can be saved; one is the default for questions. Syncing the file to the owner's Drive
 * (appDataFolder, F423) comes next; the vault key already travels in Block Store.
 */
class AiKeyStore(context: Context, private val vault: KeyVault) {

    private val file = File(File(context.filesDir, SECRETS_DIR), FILE_NAME)
    private val mutex = Mutex()
    private val stored = MutableStateFlow(StoredKeys())
    private val summaries = MutableStateFlow<List<SavedProvider>>(emptyList())

    /** Saved providers, the default first, then by name. */
    val providers: StateFlow<List<SavedProvider>> = summaries.asStateFlow()

    private var loaded = false

    /** Reads the file once; every other call does it too, so this only fills [providers] early. */
    suspend fun load() = mutex.withLock { ensureLoaded() }

    private suspend fun ensureLoaded() {
        if (loaded) return
        publish(withContext(Dispatchers.IO) { read() })
        loaded = true
    }

    /** Encrypts [apiKey] and saves it with its provider, endpoint and model; replaces an earlier key of that provider. */
    suspend fun save(providerId: String, name: String, endpoint: AiEndpoint, modelId: String, apiKey: String) = mutex.withLock {
        ensureLoaded()
        val vaultKey = checkNotNull(vault.vaultKey(createIfMissing = true)) { "No vault key" }
        val entry = StoredProvider(providerId, name, endpoint, modelId, vault.encrypt(vaultKey, apiKey, associatedData(providerId)))
        val current = stored.value
        val providers = current.providers.filter { it.providerId != providerId } + entry
        write(StoredKeys(current.defaultProviderId ?: providerId, providers))
    }

    /** The model questions go to, for a provider already saved. */
    suspend fun setModel(providerId: String, modelId: String) = mutex.withLock {
        ensureLoaded()
        val current = stored.value
        write(current.copy(providers = current.providers.map { if (it.providerId == providerId) it.copy(modelId = modelId) else it }))
    }

    suspend fun setDefault(providerId: String) = mutex.withLock {
        ensureLoaded()
        if (stored.value.providers.any { it.providerId == providerId }) write(stored.value.copy(defaultProviderId = providerId))
    }

    /** Deletes the provider and its key from this phone; the next provider becomes the default if needed. */
    suspend fun remove(providerId: String) = mutex.withLock {
        ensureLoaded()
        val current = stored.value
        val providers = current.providers.filter { it.providerId != providerId }
        val default = current.defaultProviderId.takeIf { id -> providers.any { it.providerId == id } } ?: providers.firstOrNull()?.providerId
        write(StoredKeys(default, providers))
    }

    /** The plain key of [providerId], or of the default provider when null. Held in memory only by the caller. */
    suspend fun key(providerId: String? = null): KeyLookup = mutex.withLock {
        ensureLoaded()
        val current = stored.value
        val id = providerId ?: current.defaultProviderId ?: return@withLock KeyLookup.Missing
        val entry = current.providers.firstOrNull { it.providerId == id } ?: return@withLock KeyLookup.Missing
        val summary = entry.toSummary(current.defaultProviderId)
        val vaultKey = try {
            vault.vaultKey(createIfMissing = false)
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Vault key unavailable on this phone", e)
            null
        } ?: return@withLock KeyLookup.Unavailable(summary)
        vault.decrypt(vaultKey, entry.keyCiphertext, associatedData(id))?.let { KeyLookup.Ready(summary, it) }
            ?: KeyLookup.Unavailable(summary)
    }

    /** "Erase everything": every saved key and the vault key itself (this phone and Block Store). */
    suspend fun eraseAll() = mutex.withLock {
        withContext(Dispatchers.IO) { file.delete() }
        vault.destroy()
        publish(StoredKeys())
    }

    private suspend fun write(keys: StoredKeys) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            // Write then rename, so a crash mid-write never leaves half a file.
            val temp = File(file.parentFile, "$FILE_NAME.tmp")
            temp.writeText(json.encodeToString(keys))
            check(temp.renameTo(file)) { "Could not replace $FILE_NAME" }
        }
        publish(keys)
    }

    private fun read(): StoredKeys = try {
        if (file.exists()) json.decodeFromString<StoredKeys>(file.readText()) else StoredKeys()
    } catch (e: SerializationException) {
        Log.w(TAG, "Unreadable key file; starting empty", e)
        StoredKeys()
    }

    private fun publish(keys: StoredKeys) {
        stored.value = keys
        summaries.value = keys.providers
            .map { it.toSummary(keys.defaultProviderId) }
            .sortedWith(compareByDescending<SavedProvider> { it.isDefault }.thenBy { it.name.lowercase() })
    }

    private fun StoredProvider.toSummary(defaultId: String?) = SavedProvider(providerId, name, endpoint, modelId, providerId == defaultId)

    private fun associatedData(providerId: String) = "issaspeaker|ai-key|$providerId"

    private companion object {
        const val TAG = "AiKeyStore"
        const val SECRETS_DIR = "secrets"
        const val FILE_NAME = "ai_keys.json"
        val json = Json { ignoreUnknownKeys = true }
    }
}
