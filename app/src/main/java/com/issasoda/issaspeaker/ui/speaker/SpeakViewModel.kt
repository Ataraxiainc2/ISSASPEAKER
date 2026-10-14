package com.issasoda.issaspeaker.ui.speaker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.issasoda.issaspeaker.IssaSpeakerApplication
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.speaker.AnnounceResult
import com.issasoda.issaspeaker.speaker.AnnouncementStyle
import com.issasoda.issaspeaker.speaker.VoiceChoice
import com.issasoda.issaspeaker.speaker.VoiceGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/** The voices to choose from: the phone's group first, then one group per AI provider that can speak. */
data class VoicesState(
    val phone: VoiceGroup? = null,
    val ai: List<VoiceGroup> = emptyList(),
    /** True while the AI providers are asked for their speech models. */
    val loadingAi: Boolean = false,
    /** The voice being previewed on the phone. */
    val previewing: VoiceChoice? = null,
)

/**
 * "Say something" on the speaker tab (catalog F091, F092, F093): a text spoken on the chosen speaker with the chosen
 * voice (phone or AI) and announcement volume, and the voice chooser with previews on the phone. Everything follows
 * the language the user chose in the app.
 */
class SpeakViewModel(app: Application) : AndroidViewModel(app) {
    private val services = app as IssaSpeakerApplication
    private val store = services.speakerStore

    val style: StateFlow<AnnouncementStyle> = store.style

    private val _voices = MutableStateFlow(VoicesState())
    val voices: StateFlow<VoicesState> = _voices.asStateFlow()

    private val _state = MutableStateFlow(TestState())
    val state: StateFlow<TestState> = _state.asStateFlow()

    init {
        loadVoices()
    }

    /** Phone voices at once; AI voices as each provider answers with its speech models. */
    fun loadVoices() {
        _voices.update { it.copy(loadingAi = true) }
        viewModelScope.launch {
            _voices.update { it.copy(phone = services.voiceCatalog.phone(locale())) }
            val ai = services.voiceCatalog.ai()
            _voices.update { it.copy(ai = ai, loadingAi = false) }
        }
    }

    fun chooseVoice(choice: VoiceChoice) = store.setVoice(choice.encode())

    fun setVolume(volume: Float) = store.setVolume(volume)

    /** Speaks the sample on the phone with [choice]. AI voices use the owner's credit, like any announcement. */
    fun preview(choice: VoiceChoice, text: String) {
        if (_voices.value.previewing != null) return
        _voices.update { it.copy(previewing = choice) }
        viewModelScope.launch {
            val result = services.speakerAnnouncer.previewOnPhone(text, choice, locale())
            _voices.update { it.copy(previewing = null) }
            if (result !is AnnounceResult.Spoken || result.fallback != null) _state.value = resultState(result)
        }
    }

    fun speak(text: String) {
        val speaker = store.speaker.value ?: return
        if (text.isBlank() || _state.value.running) return
        _state.value = TestState(running = true)
        viewModelScope.launch {
            val style = store.style.value
            val result = services.speakerAnnouncer.announce(speaker, text.trim(), VoiceChoice.decode(style.voice), style.volume, locale())
            _state.value = resultState(result)
        }
    }

    private fun resultState(result: AnnounceResult): TestState = when (result) {
        is AnnounceResult.Spoken -> result.fallback?.let { TestState(message = R.string.speak_done_fallback, arg = it) }
            ?: TestState(message = R.string.speak_done, success = true)
        AnnounceResult.NoWifi -> TestState(message = R.string.speak_no_wifi)
        AnnounceResult.SpeechFailed -> TestState(message = R.string.speak_tts_failed)
        is AnnounceResult.PlaybackFailed -> TestState(message = R.string.speak_playback_failed)
        is AnnounceResult.Connection -> TestState(message = SpeakerViewModel.problemMessage(result.problem))
    }

    /** The language the user chose in the app (it may differ from the phone's). */
    private fun locale(): Locale = getApplication<Application>().resources.configuration.locales[0]
}
