package com.issasoda.issaspeaker.speaker

import com.issasoda.issaspeaker.ai.AiClient
import com.issasoda.issaspeaker.ai.AiKeyStore
import com.issasoda.issaspeaker.ai.AiResult
import com.issasoda.issaspeaker.ai.AiVoices
import com.issasoda.issaspeaker.ai.KeyLookup
import com.issasoda.issaspeaker.ai.SavedProvider
import com.issasoda.issaspeaker.ai.VoiceStyle
import java.util.Locale

/** One voice to choose: [number] numbers the phone's voices per country, whose engine ids mean nothing to people. */
data class VoiceEntry(
    val choice: VoiceChoice,
    val name: String?,
    val number: Int?,
    val locale: Locale?,
    val offline: Boolean,
    val style: VoiceStyle?,
)

/** The phone's voices ([provider] null), or one AI provider's voices with the speech model they use. */
data class VoiceGroup(val provider: SavedProvider?, val model: String?, val voices: List<VoiceEntry>)

/**
 * Every voice the owner can use right now (catalog F092): the phone's voices for the app language, then the voices of
 * each saved AI provider whose key has a speech model (asked live from the provider, so a key without one is not
 * offered).
 */
class VoiceCatalog(private val speech: SpeechSynthesizer, private val keys: AiKeyStore, private val client: AiClient) {

    suspend fun phone(locale: Locale): VoiceGroup {
        val voices = speech.voices(locale)
        val numbers = mutableMapOf<String, Int>()
        val entries = voices.map { voice ->
            val number = (numbers[voice.locale.country] ?: 0) + 1
            numbers[voice.locale.country] = number
            VoiceEntry(VoiceChoice.Phone(voice.name), name = null, number = number, locale = voice.locale, offline = voice.offline, style = null)
        }
        val automatic = VoiceEntry(VoiceChoice.Automatic, name = null, number = null, locale = null, offline = true, style = null)
        return VoiceGroup(provider = null, model = null, voices = listOf(automatic) + entries)
    }

    suspend fun ai(): List<VoiceGroup> {
        keys.load()
        return keys.providers.value.mapNotNull { provider -> aiGroup(provider) }
    }

    private suspend fun aiGroup(provider: SavedProvider): VoiceGroup? {
        val api = AiVoices.apiFor(provider.providerId) ?: return null
        val key = (keys.key(provider.providerId) as? KeyLookup.Ready)?.apiKey ?: return null
        val models = (client.listModels(provider.endpoint, key) as? AiResult.Success)?.value ?: return null
        val model = AiVoices.preferredModel(api, models) ?: return null
        val voices = AiVoices.voices(api, model).map { voice ->
            VoiceEntry(VoiceChoice.Ai(provider.providerId, model, voice.id), voice.id.replaceFirstChar { it.uppercase() }, null, null, false, voice.style)
        }
        return VoiceGroup(provider, model, voices)
    }
}
