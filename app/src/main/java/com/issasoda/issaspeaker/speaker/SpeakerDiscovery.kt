package com.issasoda.issaspeaker.speaker

import android.content.Context
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.CastMediaControlIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A speaker seen on the network right now, and whether it is a speaker group set up in Google Home. */
data class FoundSpeaker(val speaker: Speaker, val isGroup: Boolean)

/**
 * Finds the Cast devices on the phone's Wi-Fi that can run the Default Media Receiver (catalog F089): Nest and
 * Google Home speakers, speaker groups, Chromecasts and TVs with Chromecast built in. The phone searches only while
 * [start] is active (the speaker screen is open), never in the background. Call [start] and [stop] on the main thread.
 */
class SpeakerDiscovery(context: Context) {

    private val router = MediaRouter.getInstance(context.applicationContext)
    private val selector = MediaRouteSelector.Builder()
        .addControlCategory(CastMediaControlIntent.categoryForCast(CastOptionsProvider.RECEIVER_APP_ID))
        .build()

    private val _found = MutableStateFlow<List<FoundSpeaker>>(emptyList())
    val found: StateFlow<List<FoundSpeaker>> = _found.asStateFlow()

    private val callback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
    }

    fun start() {
        router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY or MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        refresh()
    }

    fun stop() = router.removeCallback(callback)

    /** The route of a speaker found in this search, to connect to it. */
    internal fun route(deviceId: String): MediaRouter.RouteInfo? = castRoutes().firstOrNull { it.second.deviceId == deviceId }?.first

    private fun refresh() {
        _found.value = castRoutes()
            .map { (_, device) ->
                FoundSpeaker(
                    Speaker(device.deviceId, device.friendlyName, device.modelName.takeIf { it.isNotBlank() }),
                    isGroup = device.hasCapability(CastDevice.CAPABILITY_MULTIZONE_GROUP),
                )
            }
            .sortedBy { it.speaker.name.lowercase() }
    }

    private fun castRoutes(): List<Pair<MediaRouter.RouteInfo, CastDevice>> =
        router.routes
            .filter { !it.isDefault && !it.isBluetooth && it.isEnabled && it.matchesSelector(selector) }
            .mapNotNull { route -> CastDevice.getFromBundle(route.extras)?.let { route to it } }
}
