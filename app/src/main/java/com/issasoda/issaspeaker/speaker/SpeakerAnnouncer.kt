package com.issasoda.issaspeaker.speaker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.util.Locale

/** How an announcement went. */
sealed interface AnnounceResult {
    data object Spoken : AnnounceResult
    data object NoWifi : AnnounceResult
    data object SpeechFailed : AnnounceResult

    /** The speaker refused or stopped the audio; [reason] is for diagnostics only. */
    data class PlaybackFailed(val reason: String) : AnnounceResult
    data class Connection(val problem: ConnectionProblem) : AnnounceResult
}

/**
 * Says a text on the chosen speaker (catalog F091, F092, F093): the phone's text-to-speech writes a WAV file,
 * [AudioServer] offers it on the Wi-Fi for this announcement only, the speaker's Default Media Receiver plays it at
 * the announcement volume, and then the server closes, the file is deleted, the speaker's volume is restored and the
 * session ends. Call from the main thread.
 */
class SpeakerAnnouncer(
    private val context: Context,
    private val connection: SpeakerConnection,
    private val speech: SpeechSynthesizer,
) {

    suspend fun announce(speaker: Speaker, text: String, voice: String?, volume: Float, locale: Locale): AnnounceResult {
        val address = wifiAddress() ?: return AnnounceResult.NoWifi
        val file = File(File(context.filesDir, AUDIO_DIR), "announcement.wav")
        try {
            if (!speech.toFile(text.take(MAX_CHARS), voice, locale, file)) return AnnounceResult.SpeechFailed
            return AudioServer.open(file, address).use { server ->
                val result = connection.withSession(speaker) { session -> play(session, server.url, volume) }
                when (result) {
                    is SessionResult.Done -> result.value
                    is SessionResult.Problem -> AnnounceResult.Connection(result.problem)
                }
            }
        } finally {
            withContext(Dispatchers.IO) { file.delete() }
        }
    }

    private suspend fun play(session: CastSession, url: String, volume: Float): AnnounceResult {
        val client = session.remoteMediaClient ?: return AnnounceResult.PlaybackFailed("NO_MEDIA_CHANNEL")
        val previousVolume = session.volume
        setVolume(session, volume.toDouble())
        val finished = CompletableDeferred<AnnounceResult>()
        val callback = PlaybackEnd(client, finished)
        client.registerCallback(callback)
        try {
            val media = MediaInfo.Builder(url)
                .setContentType(AudioServer.CONTENT_TYPE)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC).apply { putString(MediaMetadata.KEY_TITLE, TITLE) })
                .build()
            client.load(MediaLoadRequestData.Builder().setMediaInfo(media).setAutoplay(true).build())
            return withTimeoutOrNull(PLAYBACK_TIMEOUT_MS) { finished.await() } ?: AnnounceResult.PlaybackFailed("TIMEOUT")
        } finally {
            client.unregisterCallback(callback)
            setVolume(session, previousVolume)
        }
    }

    /** The end of playback: finished normally, or stopped with an error. */
    private class PlaybackEnd(private val client: RemoteMediaClient, private val finished: CompletableDeferred<AnnounceResult>) :
        RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            val status = client.mediaStatus ?: return
            if (status.playerState != MediaStatus.PLAYER_STATE_IDLE) return
            when (status.idleReason) {
                MediaStatus.IDLE_REASON_FINISHED -> finished.complete(AnnounceResult.Spoken)
                MediaStatus.IDLE_REASON_ERROR -> finished.complete(AnnounceResult.PlaybackFailed("MEDIA_ERROR"))
                MediaStatus.IDLE_REASON_CANCELED, MediaStatus.IDLE_REASON_INTERRUPTED ->
                    finished.complete(AnnounceResult.PlaybackFailed("INTERRUPTED"))
                else -> Unit
            }
        }
    }

    private fun setVolume(session: CastSession, volume: Double) {
        try {
            session.volume = volume.coerceIn(0.0, 1.0)
        } catch (e: IOException) {
            Log.w(TAG, "Speaker volume not changed", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Speaker volume not changed", e)
        }
    }

    /** The phone's IPv4 address on Wi-Fi: the only interface the audio server listens on. */
    private fun wifiAddress(): InetAddress? {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivity.activeNetwork ?: return null
        if (connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return null
        return connectivity.getLinkProperties(network)?.linkAddresses?.map { it.address }?.firstOrNull { it is Inet4Address }
    }

    private companion object {
        const val TAG = "SpeakerAnnouncer"
        const val AUDIO_DIR = "cast_audio"
        const val TITLE = "ISSASPEAKER"
        const val MAX_CHARS = 2_000
        const val PLAYBACK_TIMEOUT_MS = 180_000L
    }
}
