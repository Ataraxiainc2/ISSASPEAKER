package com.issasoda.issaspeaker.ai

/** The text-to-speech APIs the app speaks. Both handle English, Spanish and Portuguese. */
enum class AiSpeechApi {
    /** `POST {base}/audio/speech` → WAV. */
    OPENAI_SPEECH,

    /** Gemini `generateContent` with an AUDIO response → 24 kHz PCM, wrapped as WAV by the app. */
    GEMINI,
}

/** How a voice sounds, as its provider describes it; shown translated. */
enum class VoiceStyle {
    BRIGHT, UPBEAT, INFORMATIVE, FIRM, EXCITABLE, YOUTHFUL, BREEZY, EASY_GOING, BREATHY, CLEAR, SMOOTH, GRAVELLY,
    SOFT, EVEN, MATURE, FORWARD, FRIENDLY, CASUAL, GENTLE, LIVELY, KNOWLEDGEABLE, WARM,
}

/** One AI voice: the provider's voice id and, when the provider documents it, its style. */
data class AiVoice(val id: String, val style: VoiceStyle?)

/**
 * Which providers can speak, through which API, with which voices (catalog F092). Like [SdkFamilies], this table
 * describes APIs, not what a key can do: the models come live from the provider's own `/models` list with the
 * owner's key, and a provider is offered only when that list has a speech model. Voice names are the ones each
 * provider documents (checked 2026-10-14); a key is still only ever sent to the endpoint saved with it.
 */
object AiVoices {

    private val gemini = listOf(
        "Zephyr" to VoiceStyle.BRIGHT, "Puck" to VoiceStyle.UPBEAT, "Charon" to VoiceStyle.INFORMATIVE,
        "Kore" to VoiceStyle.FIRM, "Fenrir" to VoiceStyle.EXCITABLE, "Leda" to VoiceStyle.YOUTHFUL,
        "Orus" to VoiceStyle.FIRM, "Aoede" to VoiceStyle.BREEZY, "Callirrhoe" to VoiceStyle.EASY_GOING,
        "Autonoe" to VoiceStyle.BRIGHT, "Enceladus" to VoiceStyle.BREATHY, "Iapetus" to VoiceStyle.CLEAR,
        "Umbriel" to VoiceStyle.EASY_GOING, "Algieba" to VoiceStyle.SMOOTH, "Despina" to VoiceStyle.SMOOTH,
        "Erinome" to VoiceStyle.CLEAR, "Algenib" to VoiceStyle.GRAVELLY, "Rasalgethi" to VoiceStyle.INFORMATIVE,
        "Laomedeia" to VoiceStyle.UPBEAT, "Achernar" to VoiceStyle.SOFT, "Alnilam" to VoiceStyle.FIRM,
        "Schedar" to VoiceStyle.EVEN, "Gacrux" to VoiceStyle.MATURE, "Pulcherrima" to VoiceStyle.FORWARD,
        "Achird" to VoiceStyle.FRIENDLY, "Zubenelgenubi" to VoiceStyle.CASUAL, "Vindemiatrix" to VoiceStyle.GENTLE,
        "Sadachbia" to VoiceStyle.LIVELY, "Sadaltager" to VoiceStyle.KNOWLEDGEABLE, "Sulafat" to VoiceStyle.WARM,
    ).map { (id, style) -> AiVoice(id, style) }

    /** gpt-4o-mini-tts and newer; tts-1 has the first nine only. */
    private val openAi = listOf(
        "alloy", "ash", "coral", "echo", "fable", "nova", "onyx", "sage", "shimmer", "ballad", "verse", "marin", "cedar",
    )
    private const val TTS1_VOICES = 9

    fun apiFor(providerId: String): AiSpeechApi? = when (providerId) {
        "openai" -> AiSpeechApi.OPENAI_SPEECH
        "google" -> AiSpeechApi.GEMINI
        else -> null
    }

    /** The speech model to use from the provider's live list: the cheaper, newer one first. Null: no speech model. */
    fun preferredModel(api: AiSpeechApi, modelIds: List<String>): String? {
        // Gemini's OpenAI-compatible list names models "models/gemini-…"; requests use the bare id.
        val speech = modelIds.map { it.removePrefix("models/") }.filter { "tts" in it.lowercase() && "realtime" !in it }
        return when (api) {
            AiSpeechApi.OPENAI_SPEECH ->
                speech.sortedWith(compareByDescending<String> { "gpt" in it }.thenByDescending { "mini" in it }.thenByDescending { it })
            // Flash (fast, affordable) but not Lite (lower quality), newest, stable before preview.
            AiSpeechApi.GEMINI -> speech.sortedWith(
                compareByDescending<String> { "flash" in it }
                    .thenBy { "lite" in it }
                    .thenByDescending { versionOf(it) }
                    .thenBy { "preview" in it },
            )
        }.firstOrNull()
    }

    fun voices(api: AiSpeechApi, model: String): List<AiVoice> = when (api) {
        AiSpeechApi.GEMINI -> gemini
        AiSpeechApi.OPENAI_SPEECH -> (if (model.startsWith("tts-1")) openAi.take(TTS1_VOICES) else openAi).map { AiVoice(it, null) }
    }

    /** "gemini-3.1-flash-tts-preview" → 3.1; models without a number sort last. */
    private fun versionOf(id: String): Double = Regex("""(\d+(\.\d+)?)""").find(id)?.value?.toDoubleOrNull() ?: 0.0
}
