package com.issasoda.issaspeaker.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/**
 * AI voices (catalog F092): turns text into a WAV file with the owner's key, on the endpoint saved with that key.
 * The text is the announcement itself, never data beyond it.
 */
class AiSpeech(baseClient: OkHttpClient) {

    private val client = baseClient.newBuilder()
        .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_S, TimeUnit.SECONDS)
        .build()

    suspend fun toFile(
        api: AiSpeechApi,
        endpoint: AiEndpoint,
        apiKey: String,
        model: String,
        voice: String,
        text: String,
        file: File,
    ): AiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = when (api) {
                    AiSpeechApi.OPENAI_SPEECH -> openAiRequest(endpoint, apiKey, model, voice, text)
                    AiSpeechApi.GEMINI -> geminiRequest(endpoint, apiKey, model, voice, text)
                }
                client.newCall(request).execute().use { response ->
                    val body = response.body.bytes()
                    if (!response.isSuccessful) {
                        val message = AiClient.errorMessage(String(body)) ?: "HTTP ${response.code}"
                        return@withContext AiResult.Failure(message, response.code)
                    }
                    val wav = when (api) {
                        AiSpeechApi.OPENAI_SPEECH -> body
                        AiSpeechApi.GEMINI -> geminiWav(String(body)) ?: return@withContext AiResult.Failure("No audio in the answer")
                    }
                    file.parentFile?.mkdirs()
                    file.writeBytes(wav)
                    AiResult.Success(Unit)
                }
            } catch (e: IOException) {
                AiResult.Failure(e.message ?: e.javaClass.simpleName)
            } catch (e: SerializationException) {
                AiResult.Failure("Unreadable answer: ${e.message}")
            } catch (e: IllegalArgumentException) {
                AiResult.Failure("Unexpected answer: ${e.message}")
            }
        }

    private fun openAiRequest(endpoint: AiEndpoint, apiKey: String, model: String, voice: String, text: String): Request {
        val payload = buildJsonObject {
            put("model", model)
            put("voice", voice)
            put("input", text)
            put("response_format", "wav")
            if (!model.startsWith("tts-1")) put("instructions", OPENAI_INSTRUCTIONS)
        }
        return Request.Builder()
            .url("${endpoint.baseUrl}/audio/speech")
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toString().toRequestBody(JSON_TYPE))
            .build()
    }

    /** Gemini speaks through its native API on the same host as the OpenAI-compatible endpoint saved with the key. */
    private fun geminiRequest(endpoint: AiEndpoint, apiKey: String, model: String, voice: String, text: String): Request {
        val payload = buildJsonObject {
            putJsonArray("contents") { addJsonObject { putJsonArray("parts") { addJsonObject { put("text", text) } } } }
            putJsonObject("generationConfig") {
                putJsonArray("responseModalities") { add(JsonPrimitive("AUDIO")) }
                putJsonObject("speechConfig") {
                    putJsonObject("voiceConfig") { putJsonObject("prebuiltVoiceConfig") { put("voiceName", voice) } }
                }
            }
        }
        return Request.Builder()
            .url("${endpoint.baseUrl.removeSuffix("/openai")}/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(payload.toString().toRequestBody(JSON_TYPE))
            .build()
    }

    companion object {
        private const val READ_TIMEOUT_S = 90L
        private const val CALL_TIMEOUT_S = 120L
        private const val DEFAULT_RATE = 24_000
        private val JSON_TYPE = "application/json".toMediaType()
        private val json = Json { ignoreUnknownKeys = true }

        private const val OPENAI_INSTRUCTIONS =
            "Read this as a warm, clear announcement for a family at home, in the language of the text."

        /** The base64 PCM in Gemini's answer, as a WAV file. */
        internal fun geminiWav(body: String): ByteArray? {
            val part = json.parseToJsonElement(body).jsonObject["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
            val inline = (part?.get("inlineData") as? JsonObject) ?: return null
            val data = (inline["data"] as? JsonPrimitive)?.contentOrNull ?: return null
            val mime = (inline["mimeType"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            val rate = Regex("""rate=(\d+)""").find(mime)?.groupValues?.get(1)?.toIntOrNull() ?: DEFAULT_RATE
            return pcmToWav(java.util.Base64.getDecoder().decode(data), rate)
        }

        /** 16-bit little-endian mono PCM → a WAV file (44-byte RIFF header + the samples). */
        fun pcmToWav(pcm: ByteArray, sampleRate: Int): ByteArray {
            val channels = 1
            val bitsPerSample = 16
            val byteRate = sampleRate * channels * bitsPerSample / BITS_PER_BYTE
            return ByteBuffer.allocate(WAV_HEADER_BYTES + pcm.size).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray())
                putInt(WAV_HEADER_BYTES - RIFF_PREAMBLE + pcm.size)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(FMT_CHUNK_BYTES)
                putShort(PCM_FORMAT)
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort((channels * bitsPerSample / BITS_PER_BYTE).toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray())
                putInt(pcm.size)
                put(pcm)
            }.array()
        }

        private const val WAV_HEADER_BYTES = 44
        private const val RIFF_PREAMBLE = 8
        private const val FMT_CHUNK_BYTES = 16
        private const val PCM_FORMAT: Short = 1
        private const val BITS_PER_BYTE = 8
    }
}
