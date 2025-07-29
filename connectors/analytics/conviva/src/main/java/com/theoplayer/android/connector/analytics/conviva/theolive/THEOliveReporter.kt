package com.theoplayer.android.connector.analytics.conviva.theolive

import android.util.Log
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.theolive.EndpointLoadedEvent
import com.theoplayer.android.api.event.player.theolive.IntentToFallbackEvent
import com.theoplayer.android.api.event.player.theolive.TheoLiveEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.connector.analytics.conviva.BuildConfig
import com.theoplayer.android.connector.analytics.conviva.utils.flattenErrorObject

private const val TAG = "THEOliveReporter"

class THEOliveReporter(val player: Player, val convivaVideoAnalytics: ConvivaVideoAnalytics) {

    private val onEndPointLoaded: EventListener<EndpointLoadedEvent> =
        EventListener<EndpointLoadedEvent> { event ->
            val endpoint = event.getEndpoint()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onEndPointLoaded ${endpoint.hespSrc}")
            }
            convivaVideoAnalytics.reportPlaybackEvent("endpointLoaded", mutableMapOf<String, Any>().apply {
                endpoint.adSrc?.let { put("adSrc", it) }
                endpoint.contentProtection?.let {
                    put("contentProtection", it)
                }
                endpoint.hespSrc?.let { put("hespSrc", it) }
                endpoint.hlsSrc?.let { put("hlsSrc", it) }
                endpoint.targetLatency?.let { put("targetLatency", it) }
                put("weight", endpoint.weight)
                put("priority", endpoint.priority)
            })
        }
    private val onIntentToFallback: EventListener<IntentToFallbackEvent> =
        EventListener<IntentToFallbackEvent> { event ->
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "IntentToFallbackEvent")
            }
            convivaVideoAnalytics.reportPlaybackEvent(
                "intentToFallback",
                event.reason?.let { flattenErrorObject(it) }
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