package com.theoplayer.android.connector.analytics.conviva.theolive

import android.util.Log
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.theolive.EndpointLoadedEvent
import com.theoplayer.android.api.event.player.theolive.IntentToFallbackEvent
import com.theoplayer.android.api.event.player.theolive.TheoLiveEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.connector.analytics.conviva.BuildConfig
import com.theoplayer.android.connector.analytics.conviva.ConvivaMetadata

private const val TAG = "THEOliveReporter"

class THEOliveReporter(val player: Player, val convivaVideoAnalytics: ConvivaVideoAnalytics) {

    private val onEndPointLoaded: EventListener<EndpointLoadedEvent> =
        EventListener<EndpointLoadedEvent> { event ->
            val endpoint = event.getEndpoint()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onEndPointLoaded - endpoint: $endpoint")
            }
            convivaVideoAnalytics.reportPlaybackEvent(
                "endpointLoaded",
                mutableMapOf<String, Any>().apply { put("endpoint", endpoint) }
            )
            endpoint.cdn?.let { cdn ->
                convivaVideoAnalytics.setContentInfo(
                    mutableMapOf(ConvivaSdkConstants.DEFAULT_RESOURCE to cdn) as ConvivaMetadata
                )
            }
        }
    private val onIntentToFallback: EventListener<IntentToFallbackEvent> =
        EventListener<IntentToFallbackEvent> { event ->
            val reason = event.reason ?: "NA"
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "IntentToFallbackEvent - reason: $reason")
            }
            convivaVideoAnalytics.reportPlaybackEvent(
                "intentToFallback",
                mutableMapOf<String, Any>().apply { put("reason", reason) }
            )
        }

    init {
        addEventListeners()
    }

    fun destroy() {
        removeEventListeners()
    }

    private fun addEventListeners() {
        player.theoLive.addEventListener(TheoLiveEventTypes.ENDPOINTLOADED, onEndPointLoaded)
        player.theoLive.addEventListener(TheoLiveEventTypes.INTENTTOFALLBACK, onIntentToFallback)
    }

    private fun removeEventListeners() {
        player.theoLive.removeEventListener(TheoLiveEventTypes.ENDPOINTLOADED, onEndPointLoaded)
        player.theoLive.removeEventListener(TheoLiveEventTypes.INTENTTOFALLBACK, onIntentToFallback)
    }
}