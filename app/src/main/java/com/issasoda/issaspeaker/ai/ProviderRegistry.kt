package com.issasoda.issaspeaker.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/** One AI provider as the live registry describes it. Only providers the app can talk to are kept. */
@Serializable
data class RegistryProvider(
    val id: String,
    val name: String,
    val doc: String? = null,
    val endpoint: AiEndpoint,
    /** What the registry knows about the provider's models, by model id. A key's real list comes from the provider. */
    val models: Map<String, RegistryModel> = emptyMap(),
    /**
     * How widely the AI world serves this provider's own models: the number of (other provider, model) pairs whose
     * canonical model belongs to it. The labs that make the models rank high; resellers that only host other labs'
     * models stay at 0. Measured from the registry, never hard-coded.
     */
    val reach: Int = 0,
)

/** What models.dev says about one model: [family] groups models of a line (gpt, claude, gemini...). */
@Serializable
data class RegistryModel(
    val name: String,
    val textOutput: Boolean,
    val releaseDate: String? = null,
    val family: String? = null,
    val contextTokens: Long? = null,
)

/**
 * The list of AI providers, live from [models.dev](https://models.dev) (catalog F416; design
 * docs/design/AI-PROVIDERS.md): no provider list ships with the app. Downloaded at most once a day and kept in a
 * compact form; a stale copy is used only when the phone is offline.
 *
 * The registry decides what is listed, never where a saved key goes: the base URL is stored with the key when the
 * user adds it ([AiKeyStore]), and the major providers' hosts are pinned ([PinnedHosts]).
 */
class ProviderRegistry(private val context: Context, private val client: OkHttpClient) {

    private val mutex = Mutex()
    private var memory: List<RegistryProvider>? = null

    /** Providers sorted by name, or the reason none could be loaded. */
    suspend fun providers(): Result<List<RegistryProvider>> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val cache = File(context.cacheDir, CACHE_FILE)
            val fresh = cache.exists() && System.currentTimeMillis() - cache.lastModified() < TTL_MS
            memory?.takeIf { fresh }?.let { return@withContext Result.success(it) }
            if (fresh) readCache(cache)?.let { return@withContext Result.success(it.also { list -> memory = list }) }

            try {
                val providers = download()
                cache.writeText(json.encodeToString(providers))
                memory = providers
                Result.success(providers)
            } catch (e: IOException) {
                fallBackToCache(cache, e)
            } catch (e: SerializationException) {
                fallBackToCache(cache, IOException("Unreadable models.dev registry", e))
            } catch (e: IllegalArgumentException) {
                fallBackToCache(cache, IOException("Unexpected models.dev registry shape", e))
            }
        }
    }

    /** The registry entry for [providerId], from the loaded list (no download). */
    fun cached(providerId: String): RegistryProvider? = memory?.firstOrNull { it.id == providerId }

    /** Offline or broken download: the last good copy, however old, beats no list at all. */
    private fun fallBackToCache(cache: File, error: IOException): Result<List<RegistryProvider>> {
        Log.w(TAG, "Registry download failed", error)
        return readCache(cache)?.let { Result.success(it.also { list -> memory = list }) } ?: Result.failure(error)
    }

    private fun readCache(cache: File): List<RegistryProvider>? = try {
        if (cache.exists()) json.decodeFromString<List<RegistryProvider>>(cache.readText()) else null
    } catch (e: SerializationException) {
        Log.w(TAG, "Unreadable registry cache", e)
        null
    }

    private fun download(): List<RegistryProvider> {
        val body = client.newCall(Request.Builder().url(REGISTRY_URL).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("models.dev answered HTTP ${response.code}")
            // A declared or actual size far beyond the registry's is not the registry.
            response.body.takeIf { it.contentLength() <= MAX_BYTES }?.string()?.takeIf { it.length <= MAX_BYTES }
                ?: throw IOException("models.dev registry too large")
        }
        return parse(body)
    }

    companion object {
        private const val TAG = "ProviderRegistry"
        private const val REGISTRY_URL = "https://models.dev/api.json"
        private const val CACHE_FILE = "ai_registry_v1.json"
        private const val TTL_MS = 24L * 60 * 60 * 1000

        /** The registry is ~6 MB today; anything far larger is not the registry. */
        private const val MAX_BYTES = 40_000_000L

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * models.dev's api.json → the providers the app can reach, sorted by name. Never renders or executes a field.
         * A pinned provider whose host changed is dropped ([PinnedHosts]).
         */
        internal fun parse(body: String): List<RegistryProvider> {
            val root = Json.parseToJsonElement(body).jsonObject
            val reach = reachByLab(root)
            return root.mapNotNull { (id, element) ->
                val provider = element as? JsonObject ?: return@mapNotNull null
                val endpoint = SdkFamilies.endpointFor(provider.string("npm"), provider.string("api"))
                    ?.takeIf { PinnedHosts.allows(id, it) } ?: return@mapNotNull null
                RegistryProvider(
                    id = id,
                    name = provider.string("name") ?: id,
                    doc = provider.string("doc")?.takeIf { it.startsWith("https://") },
                    endpoint = endpoint,
                    models = models(provider),
                    reach = reach[id] ?: 0,
                )
            }.sortedBy { it.name.lowercase() }
        }

        private fun models(provider: JsonObject): Map<String, RegistryModel> =
            (provider["models"] as? JsonObject).orEmpty().mapNotNull { (modelId, modelElement) ->
                val model = modelElement as? JsonObject ?: return@mapNotNull null
                val output = (model["modalities"] as? JsonObject)?.get("output")?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                modelId to RegistryModel(
                    name = model.string("name") ?: modelId,
                    textOutput = output.isEmpty() || "text" in output,
                    releaseDate = model.string("release_date"),
                    family = model.string("family"),
                    contextTokens = (model["limit"] as? JsonObject)?.string("context")?.toLongOrNull(),
                )
            }.toMap()

        /**
         * For each lab id, how many (provider, canonical model) pairs serve one of its models: models.dev links every
         * hosted copy to its original through `canonical_model_id` ("anthropic/claude-..."). A lab's own listing is
         * not counted, so only other providers' choices raise it.
         */
        private fun reachByLab(root: JsonObject): Map<String, Int> =
            root.flatMap { (hostId, element) ->
                ((element as? JsonObject)?.get("models") as? JsonObject).orEmpty().values
                    .mapNotNull { (it as? JsonObject)?.string("canonical_model_id") }
                    .distinct()
                    .map { it.substringBefore("/") }
                    .filter { it != hostId }
            }.groupingBy { it }.eachCount()

        private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    }
}
