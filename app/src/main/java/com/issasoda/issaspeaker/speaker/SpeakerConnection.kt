package com.issasoda.issaspeaker.speaker

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastStatusCodes
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.ModuleUnavailableException
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** The outcome of connecting to the speaker. */
sealed interface ConnectionResult {
    data object Connected : ConnectionResult

    /** The speaker was not seen in the current search (off, asleep or on another network). */
    data object NotFound : ConnectionResult

    data object TimedOut : ConnectionResult

    /** [reason] is the Cast status name (for the diagnostics screen); never personal data. */
    data class Failed(val reason: String) : ConnectionResult
}

/**
 * Opens a Cast session with the Default Media Receiver on the chosen speaker. For now it only proves the connection
 * (the speaker plays its "connected" sound) and closes it; speaking through the speaker (F091) builds on it.
 * Call from the main thread.
 */
class SpeakerConnection(private val context: Context, private val discovery: SpeakerDiscovery) {

    /** Connects to [speaker], waits for the session, holds it briefly so the speaker's sound is heard, then ends it. */
    suspend fun test(speaker: Speaker): ConnectionResult {
        val route = discovery.route(speaker.deviceId) ?: return ConnectionResult.NotFound
        val cast = try {
            CastContext.getSharedInstance(context, ContextCompat.getMainExecutor(context)).await()
        } catch (e: ModuleUnavailableException) {
            Log.w(TAG, "Cast is not available on this phone", e)
            return ConnectionResult.Failed("CAST_UNAVAILABLE")
        }
        val outcome = CompletableDeferred<ConnectionResult>()
        val listener = SessionOutcome(outcome)
        cast.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
        try {
            MediaRouter.getInstance(context).selectRoute(route)
            val result = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { outcome.await() } ?: ConnectionResult.TimedOut
            if (result == ConnectionResult.Connected) delay(HOLD_MS)
            return result
        } finally {
            cast.sessionManager.removeSessionManagerListener(listener, CastSession::class.java)
            cast.sessionManager.endCurrentSession(true)
        }
    }

    /** Completes [outcome] with the first session start or failure. */
    private class SessionOutcome(private val outcome: CompletableDeferred<ConnectionResult>) : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            outcome.complete(ConnectionResult.Connected)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            outcome.complete(ConnectionResult.Failed(CastStatusCodes.getStatusCodeString(error)))
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            outcome.complete(ConnectionResult.Connected)
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            outcome.complete(ConnectionResult.Failed(CastStatusCodes.getStatusCodeString(error)))
        }

        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionEnded(session: CastSession, error: Int) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
    }

    private companion object {
        const val TAG = "SpeakerConnection"
        const val CONNECT_TIMEOUT_MS = 20_000L
        const val HOLD_MS = 1_500L
    }
}
