package com.issasoda.issaspeaker.speaker

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A Cast device on the home network. [deviceId] is the Cast device ID, stable across renames and IP changes. */
data class Speaker(val deviceId: String, val name: String, val model: String?)

/** How announcements sound: the phone voice ([voice] null = the engine's default for the app language) and volume. */
data class AnnouncementStyle(val voice: String?, val volume: Float)

/**
 * The speaker the owner chose (catalog F090) and how announcements sound (F092, F093). The speaker is stored by its
 * Cast device ID, not by name or IP address, so another device that takes the same name on the network is not
 * mistaken for it (threat model 5.4). Not a secret.
 */
class SpeakerStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val chosen = MutableStateFlow(read())

    val speaker: StateFlow<Speaker?> = chosen.asStateFlow()

    private val styleState = MutableStateFlow(AnnouncementStyle(prefs.getString(KEY_VOICE, null), prefs.getFloat(KEY_VOLUME, DEFAULT_VOLUME)))
    val style: StateFlow<AnnouncementStyle> = styleState.asStateFlow()

    fun setVoice(voice: String?) {
        prefs.edit { putString(KEY_VOICE, voice) }
        styleState.value = styleState.value.copy(voice = voice)
    }

    fun setVolume(volume: Float) {
        prefs.edit { putFloat(KEY_VOLUME, volume) }
        styleState.value = styleState.value.copy(volume = volume)
    }

    fun choose(speaker: Speaker) {
        prefs.edit {
            putString(KEY_ID, speaker.deviceId)
            putString(KEY_NAME, speaker.name)
            putString(KEY_MODEL, speaker.model)
        }
        chosen.value = speaker
    }

    /** "Forget this speaker": the voice and volume stay. */
    fun forget() {
        prefs.edit {
            remove(KEY_ID)
            remove(KEY_NAME)
            remove(KEY_MODEL)
        }
        chosen.value = null
    }

    /** Part of "Erase everything". */
    fun clear() {
        prefs.edit { clear() }
        chosen.value = null
        styleState.value = AnnouncementStyle(null, DEFAULT_VOLUME)
    }

    private fun read(): Speaker? {
        val id = prefs.getString(KEY_ID, null) ?: return null
        return Speaker(id, prefs.getString(KEY_NAME, null) ?: id, prefs.getString(KEY_MODEL, null))
    }

    private companion object {
        const val FILE = "speaker"
        const val KEY_ID = "device_id"
        const val KEY_NAME = "name"
        const val KEY_MODEL = "model"
        const val KEY_VOICE = "voice"
        const val KEY_VOLUME = "volume"
        const val DEFAULT_VOLUME = 0.5f
    }
}
