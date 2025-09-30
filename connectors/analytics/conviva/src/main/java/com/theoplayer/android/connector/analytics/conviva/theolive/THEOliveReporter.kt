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
import com.theoplayer.android.connector.analytics.conviva.utils.flattenErrorObject

private const val TAG = "THEOliveReporter"

class THEOliveReporter(val player: Player, val convivaVideoAnalytics: ConvivaVideoAnalytics) {

    private val onEndPointLoaded =
        EventListener<EndpointLoadedEvent> { handleEndPointLoaded(it) }
    private val onIntentToFallback =
        EventListener<IntentToFallbackEvent> { handleIntentToFallback(it) }

    init {
        addEventListeners()
    }

    private fun handleEndPointLoaded(event: EndpointLoadedEvent) {
        val endpoint = event.getEndpoint()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onEndPointLoaded - endpoint: $endpoint")
        }

        convivaVideoAnalytics.setContentInfo(mutableMapOf<String, String>().apply {
            endpoint.cdn?.let { cdn ->
                put(ConvivaSdkConstants.DEFAULT_RESOURCE, cdn)
            }
        } as ConvivaMetadata)
    }

    private fun handleIntentToFallback(event: IntentToFallbackEvent) {
        val reason = event.reason
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "IntentToFallbackEvent - reason: ${reason ?: "NA"}")
        }
        convivaVideoAnalytics.reportPlaybackEvent(
            "intentToFallback", mutableMapOf<String, Any>().apply {
                reason?.let {
                    put("reason", flattenErrorObject(reason))
                }
            })
    }

    private fun addEventListeners() {
        player.theoLive.addEventListener(TheoLiveEventTypes.ENDPOINTLOADED, onEndPointLoaded)
        player.theoLive.addEventListener(TheoLiveEventTypes.INTENTTOFALLBACK, onIntentToFallback)
    }

    private fun removeEventListeners() {
        player.theoLive.removeEventListener(TheoLiveEventTypes.ENDPOINTLOADED, onEndPointLoaded)
        player.theoLive.removeEventListener(TheoLiveEventTypes.INTENTTOFALLBACK, onIntentToFallback)
    }

    fun destroy() {
        removeEventListeners()
    }
}