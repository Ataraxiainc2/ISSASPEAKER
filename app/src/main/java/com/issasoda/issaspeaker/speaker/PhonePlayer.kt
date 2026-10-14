package com.issasoda.issaspeaker.speaker

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume

/** Plays a short audio file on the phone itself (voice previews), one at a time. */
object PhonePlayer {

    /**
     * The player of the preview in progress. Held here on purpose: a MediaPlayer that nothing references can be
     * garbage-collected mid-playback, which silences it (the "first preview plays nothing" bug).
     */
    private var current: MediaPlayer? = null

    /** True when [file] played to the end. */
    suspend fun play(file: File): Boolean = suspendCancellableCoroutine { continuation ->
        current?.release()
        val player = MediaPlayer()
        current = player
        var finished = false
        fun finish(played: Boolean) {
            if (finished) return
            finished = true
            player.release()
            if (current === player) current = null
            if (continuation.isActive) continuation.resume(played)
        }
        continuation.invokeOnCancellation { finish(false) }
        player.setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
        )
        player.setOnCompletionListener { finish(true) }
        player.setOnErrorListener { _, _, _ ->
            finish(false)
            true
        }
        try {
            player.setDataSource(file.path)
            player.setOnPreparedListener { it.start() }
            player.prepareAsync()
        } catch (_: IOException) {
            finish(false)
        } catch (_: IllegalStateException) {
            finish(false)
        }
    }
}
