package com.issasoda.issaspeaker.speaker

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

/**
 * Google Cast setup, read by the Cast SDK through the manifest. ISSASPEAKER uses Google's Default Media Receiver:
 * no receiver app of ours runs on the speaker and nothing has to be registered in the Cast console. The speaker
 * only ever plays an audio URL the phone gives it (catalog F089, F091).
 */
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(RECEIVER_APP_ID)
            // No Cast media notification or lock-screen controls: announcements are short and started by the app.
            .setCastMediaOptions(CastMediaOptions.Builder().setNotificationOptions(null).build())
            // Never rejoin a session by itself after the app closes.
            .setResumeSavedSession(false)
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null

    companion object {
        val RECEIVER_APP_ID: String = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
    }
}
