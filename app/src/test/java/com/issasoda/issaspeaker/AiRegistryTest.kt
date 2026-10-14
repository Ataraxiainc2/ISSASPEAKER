package com.issasoda.issaspeaker

import com.issasoda.issaspeaker.ai.AiClient
import com.issasoda.issaspeaker.ai.AiEndpoint
import com.issasoda.issaspeaker.ai.AiProtocol
import com.issasoda.issaspeaker.ai.PinnedHosts
import com.issasoda.issaspeaker.ai.ProviderRegistry
import com.issasoda.issaspeaker.ai.RegistryModel
import com.issasoda.issaspeaker.ai.RegistryProvider
import com.issasoda.issaspeaker.ai.SdkFamilies
import com.issasoda.issaspeaker.ui.ai.AddProviderViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRegistryTest {

    @Test
    fun `registry base URL wins, SDK default fills the gap, unknown or unsafe roots are dropped`() {
        assertEquals(
            AiEndpoint(AiProtocol.OPENAI_COMPATIBLE, "https://api.deepseek.com"),
            SdkFamilies.endpointFor("@ai-sdk/openai-compatible", "https://api.deepseek.com/"),
        )
        assertEquals(AiEndpoint(AiProtocol.ANTHROPIC, "https://api.anthropic.com/v1"), SdkFamilies.endpointFor("@ai-sdk/anthropic", null))
        assertNull(SdkFamilies.endpointFor("@ai-sdk/openai-compatible", null))
        assertNull(SdkFamilies.endpointFor("@ai-sdk/openai-compatible", "http://127.0.0.1:1234/v1"))
        assertNull(SdkFamilies.endpointFor("@ai-sdk/openai-compatible", "https://\${ACCOUNT}.example.com/v1"))
        assertNull(SdkFamilies.endpointFor("@ai-sdk/amazon-bedrock", null))
    }

    @Test
    fun `a pinned provider is only accepted on its own host`() {
        assertTrue(PinnedHosts.allows("anthropic", AiEndpoint(AiProtocol.ANTHROPIC, "https://api.anthropic.com/v1")))
        assertFalse(PinnedHosts.allows("anthropic", AiEndpoint(AiProtocol.ANTHROPIC, "https://api.anthropic.com.evil.example/v1")))
        assertFalse(PinnedHosts.allows("openai", AiEndpoint(AiProtocol.OPENAI_COMPATIBLE, "https://proxy.example/v1")))
        assertTrue(PinnedHosts.allows("some-new-lab", AiEndpoint(AiProtocol.OPENAI_COMPATIBLE, "https://api.new-lab.ai/v1")))
    }

    @Test
    fun `parse keeps reachable providers sorted by name, drops a hijacked pinned one`() {
        val body = """
            {
              "zeta": {"id": "zeta", "name": "Zeta AI", "npm": "@ai-sdk/openai-compatible", "api": "https://zeta.ai/v1",
                       "models": {"z-chat": {"name": "Z Chat", "modalities": {"input": ["text"], "output": ["text"]}},
                                  "z-image": {"name": "Z Image", "modalities": {"input": ["text"], "output": ["image"]}}}},
              "anthropic": {"id": "anthropic", "name": "Anthropic", "npm": "@ai-sdk/anthropic", "doc": "https://docs.anthropic.com",
                            "models": {}},
              "openai": {"id": "openai", "name": "OpenAI", "npm": "@ai-sdk/openai-compatible", "api": "https://keys.attacker.example/v1"},
              "bedrock": {"id": "bedrock", "name": "Bedrock", "npm": "@ai-sdk/amazon-bedrock", "models": {}},
              "local": {"id": "local", "name": "Local", "npm": "@ai-sdk/openai-compatible", "api": "http://localhost:1/v1"}
            }
        """.trimIndent()

        val providers = ProviderRegistry.parse(body)

        assertEquals(listOf("anthropic", "zeta"), providers.map { it.id })
        val zeta = providers.last()
        assertTrue(zeta.models.getValue("z-chat").textOutput)
        assertFalse(zeta.models.getValue("z-image").textOutput)
        assertEquals("https://docs.anthropic.com", providers.first().doc)
    }

    @Test
    fun `reach counts other providers serving a lab's models, not the lab's own listing`() {
        val body = """
            {
              "lab": {"name": "Lab", "npm": "@ai-sdk/openai-compatible", "api": "https://lab.ai/v1",
                      "models": {"m1": {"name": "M1", "canonical_model_id": "lab/m1"}}},
              "hostA": {"name": "Host A", "npm": "@ai-sdk/openai-compatible", "api": "https://a.ai/v1",
                        "models": {"lab/m1": {"name": "M1", "canonical_model_id": "lab/m1"},
                                   "lab/m1-fast": {"name": "M1 fast", "canonical_model_id": "lab/m1"},
                                   "lab/m2": {"name": "M2", "canonical_model_id": "lab/m2"}}},
              "hostB": {"name": "Host B", "npm": "@ai-sdk/openai-compatible", "api": "https://b.ai/v1",
                        "models": {"m1": {"name": "M1", "canonical_model_id": "lab/m1"}}}
            }
        """.trimIndent()

        val reach = ProviderRegistry.parse(body).associate { it.id to it.reach }

        assertEquals(mapOf("hostA" to 0, "hostB" to 0, "lab" to 3), reach)
    }

    @Test
    fun `provider error bodies become readable messages`() {
        assertEquals(
            "Your credit balance is too low",
            AiClient.errorMessage("""{"type":"error","error":{"type":"invalid_request_error","message":"Your credit balance is too low"}}"""),
        )
        assertEquals("Model not found", AiClient.errorMessage("""[{"error":{"code":404,"message":"Model not found"}}]"""))
        assertEquals("Payment required", AiClient.errorMessage("""{"message":"Payment required"}"""))
        assertEquals("Bad gateway", AiClient.errorMessage("Bad gateway"))
        assertNull(AiClient.errorMessage(""))
    }

    @Test
    fun `answers are read from both protocols`() {
        assertEquals("Hi", AiClient.answerText(AiProtocol.OPENAI_COMPATIBLE, """{"choices":[{"message":{"role":"assistant","content":"Hi"}}]}"""))
        assertEquals(
            "Hello there",
            AiClient.answerText(AiProtocol.ANTHROPIC, """{"content":[{"type":"text","text":"Hello "},{"type":"text","text":"there"}]}"""),
        )
    }

    @Test
    fun `gemini model ids lose their models prefix`() {
        assertEquals("gemini-flash-latest", AiClient.normalizeModelId("models/gemini-flash-latest"))
        assertEquals("gpt-5-mini", AiClient.normalizeModelId("gpt-5-mini"))
    }

    @Test
    fun `models list known text models newest first, then unknown ones, without image models`() {
        val provider = RegistryProvider(
            id = "lab",
            name = "Lab",
            endpoint = AiEndpoint(AiProtocol.OPENAI_COMPATIBLE, "https://lab.ai/v1"),
            models = mapOf(
                "old" to RegistryModel("Old", textOutput = true, releaseDate = "2025-01-01"),
                "new" to RegistryModel("New", textOutput = true, releaseDate = "2026-09-01", family = "lab"),
                "paint" to RegistryModel("Paint", textOutput = false),
            ),
        )

        val options = AddProviderViewModel.modelOptions(provider, listOf("old", "paint", "zz-unknown", "new"))

        assertEquals(listOf("new", "old", "zz-unknown"), options.map { it.id })
        assertEquals("zz", options.last().family)
    }
}
