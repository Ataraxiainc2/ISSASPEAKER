package com.issasoda.issaspeaker.ui.speaker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.issasoda.issaspeaker.IssaSpeakerApplication
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.speaker.AnnounceResult
import com.issasoda.issaspeaker.speaker.AnnouncementStyle
import com.issasoda.issaspeaker.speaker.VoiceOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * "Say something" on the speaker tab (catalog F091, F092, F093): a text spoken on the chosen speaker with the chosen
 * phone voice and announcement volume.
 */
class SpeakViewModel(app: Application) : AndroidViewModel(app) {
    private val services = app as IssaSpeakerApplication
    private val store = services.speakerStore

    val style: StateFlow<AnnouncementStyle> = store.style

    private val _voices = MutableStateFlow<List<VoiceOption>>(emptyList())

    /** The phone's voices for the app's language. */
    val voices: StateFlow<List<VoiceOption>> = _voices.asStateFlow()

    private val _state = MutableStateFlow(TestState())
    val state: StateFlow<TestState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _voices.value = services.speech.voices(locale()) }
    }

    fun setVoice(voice: String?) = store.setVoice(voice)

    fun setVolume(volume: Float) = store.setVolume(volume)

    fun speak(text: String) {
        val speaker = store.speaker.value ?: return
        if (text.isBlank() || _state.value.running) return
        _state.value = TestState(running = true)
        viewModelScope.launch {
            val style = store.style.value
            val result = services.speakerAnnouncer.announce(speaker, text.trim(), style.voice, style.volume, locale())
            _state.value = when (result) {
                AnnounceResult.Spoken -> TestState(message = R.string.speak_done, success = true)
                AnnounceResult.NoWifi -> TestState(message = R.string.speak_no_wifi)
                AnnounceResult.SpeechFailed -> TestState(message = R.string.speak_tts_failed)
                is AnnounceResult.PlaybackFailed -> TestState(message = R.string.speak_playback_failed)
                is AnnounceResult.Connection -> TestState(message = SpeakerViewModel.problemMessage(result.problem))
            }
        }
    }

    /** The app's language (the owner may have chosen one different from the phone's). */
    private fun locale(): Locale = getApplication<Application>().resources.configuration.locales[0]
}
