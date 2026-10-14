package com.issasoda.issaspeaker.ai

import kotlinx.serialization.Serializable
import java.net.URI

/** The two wire protocols the app speaks. */
@Serializable
enum class AiProtocol {
    /** `GET {base}/models`, `POST {base}/chat/completions`, `Authorization: Bearer <key>`. */
    OPENAI_COMPATIBLE,

    /** Anthropic Messages API: `GET {base}/models`, `POST {base}/messages`, `x-api-key` + `anthropic-version`. */
    ANTHROPIC,
}

/** Where and how to reach one provider. A saved key is only ever sent to the [baseUrl] stored with it. */
@Serializable
data class AiEndpoint(val protocol: AiProtocol, val baseUrl: String) {
    val host: String get() = runCatching { URI(baseUrl).host }.getOrNull().orEmpty()
}

/**
 * The wire protocol behind each AI SDK package that models.dev names in a provider's `npm` field, and the API root
 * that package uses when the registry gives none.
 *
 * This table describes protocols, not providers or models: which providers exist comes live from models.dev
 * ([ProviderRegistry]) and which models a key can use comes live from that provider's own `/models` endpoint
 * ([AiClient.listModels]). The default roots are the ones the SDK packages hard-code themselves. Same table as
 * the Movie Recommendation app, where every entry was checked against the live API on 2026-10-13. A provider
 * whose package is missing (Azure, Bedrock, Vertex and other signed or per-account APIs) is not listed.
 */
object SdkFamilies {

    private class Family(val protocol: AiProtocol, val defaultBaseUrl: String?)

    private val families = mapOf(
        // The registry gives the base URL for these.
        "@ai-sdk/openai-compatible" to Family(AiProtocol.OPENAI_COMPATIBLE, null),
        "@openrouter/ai-sdk-provider" to Family(AiProtocol.OPENAI_COMPATIBLE, null),
        // First-party SDKs: their own API root, all OpenAI-compatible except Anthropic.
        "@ai-sdk/openai" to Family(AiProtocol.OPENAI_COMPATIBLE, "https://api.openai.com/v1"),
        "@ai-sdk/google" to Family(AiProtocol.OPENAI_COMPATIBLE, "https://generativelanguage.googleapis.com/v1beta/openai"),
        "@ai-sdk/mistral" to Family(AiProtocol.OPENAI_COMPATIBLE, "https://api.mistral.ai/v1"),
        "@ai-sdk/xai" to Family(AiProtocol.OPENAI_COMPATIBLE, "https://api.x.ai/v1"),
        "@ai-sdk/groq" to Family(AiProtocol.OPENAI_COMPATIBLE, "https://api.groq.com/openai/v1"),
        "@ai-sdk/cerebras" to Family(AiProtocol.OPENAI_COMPATIBLE, "https://api.cerebras.ai/v1"),
        "@ai-sdk/cohere" to Family(AiProtocol.OPENAI_COMPATIBLE, "https://api.cohere.ai/compatibility/v1"),
        "@ai-sdk/anthropic" to Family(AiProtocol.ANTHROPIC, "https://api.anthropic.com/v1"),
    )

    /** How to reach a registry provider, or null when the app cannot talk to it. Only HTTPS roots are accepted. */
    fun endpointFor(npm: String?, api: String?): AiEndpoint? {
        val family = families[npm] ?: return null
        val base = (api?.takeIf { it.isNotBlank() } ?: family.defaultBaseUrl)?.trimEnd('/') ?: return null
        // HTTPS only; a "${ACCOUNT_URL}"-style placeholder means the provider needs per-account setup.
        if (!base.startsWith("https://") || '{' in base) return null
        return AiEndpoint(family.protocol, base)
    }
}

/**
 * Known API hosts of the major providers (catalog F418, threat model 5.3a). The registry is community data: if it
 * ever pointed one of these providers somewhere else, the provider is left out of the list instead of sending a
 * new key to an unknown host. Keys already saved never follow the registry anyway: they keep the endpoint they
 * were saved with.
 */
object PinnedHosts {

    private val hosts = mapOf(
        "anthropic" to "api.anthropic.com",
        "openai" to "api.openai.com",
        "google" to "generativelanguage.googleapis.com",
        "xai" to "api.x.ai",
        "mistral" to "api.mistral.ai",
        "groq" to "api.groq.com",
        "deepseek" to "api.deepseek.com",
        "cerebras" to "api.cerebras.ai",
        "cohere" to "api.cohere.ai",
        "openrouter" to "openrouter.ai",
    )

    /** False only for a pinned provider whose endpoint host differs from the pinned one. */
    fun allows(providerId: String, endpoint: AiEndpoint): Boolean = hosts[providerId]?.let { it == endpoint.host } ?: true
}
