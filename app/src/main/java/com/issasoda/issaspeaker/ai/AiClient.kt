package com.issasoda.issaspeaker.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** The outcome of one call to a provider: a value, or the provider's own explanation of what went wrong. */
sealed interface AiResult<out T> {
    data class Success<T>(val value: T) : AiResult<T>

    /** [message] is the provider's error text when it sent one (wrong key, no credit, unknown model...). */
    data class Failure(val message: String, val httpCode: Int? = null) : AiResult<Nothing>
}

/** One turn of a conversation. Only the owner's own words and the model's answers go here. */
data class ChatTurn(val fromUser: Boolean, val text: String)

/**
 * Talks to any registry provider with the user's key over its protocol ([AiProtocol]); catalog F419, F420. Nothing
 * is hard-coded about models: [listModels] asks the provider which models the key can use. Direct REST, no vendor SDK.
 */
class AiClient(baseClient: OkHttpClient) {

    // Completions can take a while on large models; the shared client's read timeout is too short for them.
    private val client = baseClient.newBuilder()
        .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_S, TimeUnit.SECONDS)
        .build()

    /** The model ids [apiKey] can use, as the provider lists them. A successful answer also proves the key is valid. */
    suspend fun listModels(endpoint: AiEndpoint, apiKey: String): AiResult<List<String>> {
        val path = "models" + if (endpoint.protocol == AiProtocol.ANTHROPIC) "?limit=1000" else ""
        return execute(request(endpoint, apiKey, path)) { body ->
            val root = json.parseToJsonElement(body).jsonObject
            val items = (root["data"] ?: root["models"])?.jsonArray.orEmpty()
            items.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                (obj.string("id") ?: obj.string("name"))?.let(::normalizeModelId)
            }.distinct()
        }
    }

    /** The model's answer to a conversation that ends with the owner's turn. */
    suspend fun chat(endpoint: AiEndpoint, apiKey: String, model: String, turns: List<ChatTurn>): AiResult<String> {
        val payload = buildJsonObject {
            put("model", model)
            // Anthropic requires a limit; OpenAI's newest models reject max_tokens, so the others get none.
            if (endpoint.protocol == AiProtocol.ANTHROPIC) put("max_tokens", ANTHROPIC_MAX_TOKENS)
            putJsonArray("messages") {
                turns.forEach { turn ->
                    addJsonObject {
                        put("role", if (turn.fromUser) "user" else "assistant")
                        put("content", turn.text)
                    }
                }
            }
        }
        val path = if (endpoint.protocol == AiProtocol.ANTHROPIC) "messages" else "chat/completions"
        return execute(request(endpoint, apiKey, path, payload)) { body -> answerText(endpoint.protocol, body) }
    }

    private fun request(endpoint: AiEndpoint, apiKey: String, path: String, payload: JsonObject? = null): Request =
        Request.Builder()
            .url("${endpoint.baseUrl}/$path")
            .apply {
                when (endpoint.protocol) {
                    AiProtocol.OPENAI_COMPATIBLE -> header("Authorization", "Bearer $apiKey")
                    AiProtocol.ANTHROPIC -> header("x-api-key", apiKey).header("anthropic-version", ANTHROPIC_VERSION)
                }
                if (payload != null) post(payload.toString().toRequestBody(JSON_TYPE))
            }
            .build()

    private suspend fun <T> execute(request: Request, read: (String) -> T): AiResult<T> = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (response.isSuccessful) {
                    AiResult.Success(read(body))
                } else {
                    AiResult.Failure(errorMessage(body) ?: "HTTP ${response.code}", response.code)
                }
            }
        } catch (e: IOException) {
            AiResult.Failure(e.message ?: e.javaClass.simpleName)
        } catch (e: SerializationException) {
            AiResult.Failure("Unreadable answer: ${e.message}")
        } catch (e: IllegalArgumentException) {
            AiResult.Failure("Unexpected answer: ${e.message}")
        }
    }

    internal companion object {
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val ANTHROPIC_MAX_TOKENS = 2048
        private const val READ_TIMEOUT_S = 90L
        private const val CALL_TIMEOUT_S = 120L
        private const val MAX_ERROR_CHARS = 300
        private val JSON_TYPE = "application/json".toMediaType()
        private val json = Json { ignoreUnknownKeys = true }

        /** Gemini lists "models/gemini-x" but expects "gemini-x" in requests. */
        fun normalizeModelId(id: String): String = id.removePrefix("models/")

        /** The text of a completion: the first choice (OpenAI-compatible) or every text block (Anthropic). */
        fun answerText(protocol: AiProtocol, body: String): String {
            val root = json.parseToJsonElement(body).jsonObject
            return when (protocol) {
                AiProtocol.OPENAI_COMPATIBLE ->
                    root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.string("content")
                AiProtocol.ANTHROPIC ->
                    root["content"]?.jsonArray?.mapNotNull { (it as? JsonObject)?.string("text") }?.joinToString("")
            }.orEmpty()
        }

        /** The human-readable part of a provider's error body: error.message, message, or the start of the text. */
        fun errorMessage(body: String): String? {
            val element: JsonElement? = try {
                json.parseToJsonElement(body)
            } catch (_: SerializationException) {
                null
            }
            // Gemini wraps its error object in an array.
            val root = (element as? JsonObject) ?: ((element as? JsonArray)?.firstOrNull() as? JsonObject)
            val error = root?.get("error")
            val fromJson = (error as? JsonObject)?.string("message")
                ?: (error as? JsonPrimitive)?.contentOrNull
                ?: root?.string("message")
            return (fromJson ?: body.takeIf { it.isNotBlank() })?.trim()?.take(MAX_ERROR_CHARS)
        }

        private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    }
}
