package com.issasoda.issaspeaker

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.issasoda.issaspeaker.ai.AiClient
import com.issasoda.issaspeaker.ai.AiKeyStore
import com.issasoda.issaspeaker.ai.AiSpeech
import com.issasoda.issaspeaker.ai.KeyVault
import com.issasoda.issaspeaker.ai.ProviderRegistry
import com.issasoda.issaspeaker.speaker.SpeakerAnnouncer
import com.issasoda.issaspeaker.speaker.SpeakerConnection
import com.issasoda.issaspeaker.speaker.SpeakerDiscovery
import com.issasoda.issaspeaker.speaker.SpeakerStore
import com.issasoda.issaspeaker.speaker.SpeechSynthesizer
import com.issasoda.issaspeaker.speaker.VoiceCatalog
import okhttp3.OkHttpClient

/** One instance of each long-lived service, shared by every screen. Nothing is created until first used. */
class IssaSpeakerApplication : Application(), SingletonImageLoader.Factory {

    private val http by lazy { OkHttpClient() }

    val providerRegistry by lazy { ProviderRegistry(this, http) }
    val aiClient by lazy { AiClient(http) }
    val aiKeys by lazy { AiKeyStore(this, KeyVault(this)) }

    val speakerStore by lazy { SpeakerStore(this) }
    val speakerDiscovery by lazy { SpeakerDiscovery(this) }
    val speakerConnection by lazy { SpeakerConnection(this, speakerDiscovery) }
    val speech by lazy { SpeechSynthesizer(this) }
    val speakerAnnouncer by lazy { SpeakerAnnouncer(this, speakerConnection, speech, AiSpeech(http), aiKeys) }
    val voiceCatalog by lazy { VoiceCatalog(speech, aiKeys, aiClient) }

    /** Images: the Google profile photo and the AI providers' logos (SVG, from models.dev). */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { http }))
                add(SvgDecoder.Factory())
            }
            .build()
}
