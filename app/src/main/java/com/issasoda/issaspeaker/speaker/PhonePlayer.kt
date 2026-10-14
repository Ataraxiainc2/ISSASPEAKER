package com.issasoda.issaspeaker.speaker

import android.media.MediaPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume

/** Plays a short audio file on the phone itself (voice previews). */
object PhonePlayer {

    /** True when [file] played to the end. */
    suspend fun play(file: File): Boolean = suspendCancellableCoroutine { continuation ->
        val player = MediaPlayer()
        continuation.invokeOnCancellation { player.release() }
        player.setOnCompletionListener {
            it.release()
            if (continuation.isActive) continuation.resume(true)
        }
        player.setOnErrorListener { mp, _, _ ->
            mp.release()
            if (continuation.isActive) continuation.resume(false)
            true
        }
        try {
            player.setDataSource(file.path)
            player.setOnPreparedListener { it.start() }
            player.prepareAsync()
        } catch (_: IOException) {
            player.release()
            continuation.resume(false)
        } catch (_: IllegalStateException) {
            player.release()
            continuation.resume(false)
        }
    }
}
