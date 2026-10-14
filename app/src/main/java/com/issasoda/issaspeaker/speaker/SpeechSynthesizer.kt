package com.issasoda.issaspeaker.speaker

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/** A voice the phone's text-to-speech engine offers: [name] is the engine's id; [offline] needs no internet. */
data class VoiceOption(val name: String, val locale: Locale, val offline: Boolean, val quality: Int)

/**
 * The phone's own text-to-speech (catalog F092): free, private, and works without internet for offline voices.
 * Text is turned into a WAV file that [AudioServer] hands to the speaker. One engine for the whole app, created on
 * first use.
 */
class SpeechSynthesizer(context: Context) {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var engine: TextToSpeech? = null

    /** The voices for [locale]'s language, best first: offline before network, higher quality first. */
    suspend fun voices(locale: Locale): List<VoiceOption> = mutex.withLock {
        val tts = engine() ?: return@withLock emptyList()
        tts.voices.orEmpty()
            .filter { it.locale.language == locale.language && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
            .map { VoiceOption(it.name, it.locale, offline = !it.isNetworkConnectionRequired, quality = it.quality) }
            .sortedWith(
                compareByDescending<VoiceOption> { it.locale.country == locale.country || it.locale.country in regionCountries(locale) }
                    .thenByDescending { it.offline }
                    .thenByDescending { it.quality }
                    .thenBy { it.name },
            )
    }

    /** Writes [text] spoken by [voiceName] (or the engine's voice for [locale]) to [file]; false when it failed. */
    suspend fun toFile(text: String, voiceName: String?, locale: Locale, file: File): Boolean = mutex.withLock {
        val tts = engine() ?: return@withLock false
        val voice = voiceName?.let { name -> tts.voices.orEmpty().firstOrNull { it.name == name } }
        if (voice != null) tts.voice = voice else tts.language = locale
        val utterance = UUID.randomUUID().toString()
        val done = CompletableDeferred<Boolean>()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit
            override fun onDone(utteranceId: String) {
                if (utteranceId == utterance) done.complete(true)
            }

            @Deprecated("Required override of the deprecated single-argument callback")
            override fun onError(utteranceId: String) {
                if (utteranceId == utterance) done.complete(false)
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                if (utteranceId == utterance) done.complete(false)
            }
        })
        file.parentFile?.mkdirs()
        if (tts.synthesizeToFile(text, null, file, utterance) != TextToSpeech.SUCCESS) return@withLock false
        withTimeoutOrNull(SYNTHESIS_TIMEOUT_MS) { done.await() } == true && file.length() > 0
    }

    private suspend fun engine(): TextToSpeech? = engine ?: withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            var created: TextToSpeech? = null
            created = TextToSpeech(appContext) { status ->
                continuation.resume(created.takeIf { status == TextToSpeech.SUCCESS })
            }
        }
    }.also { engine = it }

    private companion object {
        const val SYNTHESIS_TIMEOUT_MS = 60_000L

        /** Phone voices are per country; a region such as Latin America (419) prefers its countries' voices. */
        fun regionCountries(locale: Locale): Set<String> = if (locale.country == "419") setOf("US", "MX") else emptySet()
    }
}
