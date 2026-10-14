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

/** Why a session with the speaker could not be used. */
sealed interface ConnectionProblem {
    /** The speaker was not seen in the current search (off, asleep or on another network). */
    data object NotFound : ConnectionProblem

    data object TimedOut : ConnectionProblem

    /** [reason] is the Cast status name (for the diagnostics screen); never personal data. */
    data class Failed(val reason: String) : ConnectionProblem
}

/** The value a session produced, or why there was no session. */
sealed interface SessionResult<out T> {
    data class Done<T>(val value: T) : SessionResult<T>
    data class Problem(val problem: ConnectionProblem) : SessionResult<Nothing>
}

/**
 * Opens a Cast session with the Default Media Receiver on the chosen speaker, runs one piece of work in it and always
 * ends it, so the speaker is free again (catalog F089, F091). Call from the main thread.
 */
class SpeakerConnection(private val context: Context, private val discovery: SpeakerDiscovery) {

    /** Connects and holds the session briefly, so the speaker's connection sound is heard. */
    suspend fun test(speaker: Speaker): SessionResult<Unit> = withSession(speaker) { delay(HOLD_MS) }

    suspend fun <T> withSession(speaker: Speaker, work: suspend (CastSession) -> T): SessionResult<T> {
        val route = discovery.route(speaker.deviceId) ?: return SessionResult.Problem(ConnectionProblem.NotFound)
        val cast = try {
            CastContext.getSharedInstance(context, ContextCompat.getMainExecutor(context)).await()
        } catch (e: ModuleUnavailableException) {
            Log.w(TAG, "Cast is not available on this phone", e)
            return SessionResult.Problem(ConnectionProblem.Failed("CAST_UNAVAILABLE"))
        }
        val outcome = CompletableDeferred<CastSession?>()
        val failure = CompletableDeferred<String>()
        val listener = SessionOutcome(outcome, failure)
        cast.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
        try {
            MediaRouter.getInstance(context).selectRoute(route)
            val session = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { outcome.await() }
                ?: return SessionResult.Problem(if (failure.isCompleted) ConnectionProblem.Failed(failure.await()) else ConnectionProblem.TimedOut)
            return SessionResult.Done(work(session))
        } finally {
            cast.sessionManager.removeSessionManagerListener(listener, CastSession::class.java)
            cast.sessionManager.endCurrentSession(true)
        }
    }

    /** Completes [session] with the started session, or with null and [failure] with the Cast status. */
    private class SessionOutcome(
        private val session: CompletableDeferred<CastSession?>,
        private val failure: CompletableDeferred<String>,
    ) : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            this.session.complete(session)
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            this.session.complete(session)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) = fail(error)

        override fun onSessionResumeFailed(session: CastSession, error: Int) = fail(error)

        private fun fail(error: Int) {
            failure.complete(CastStatusCodes.getStatusCodeString(error))
            session.complete(null)
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
