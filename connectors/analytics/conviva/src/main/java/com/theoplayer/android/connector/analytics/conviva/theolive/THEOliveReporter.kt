package com.theoplayer.android.connector.analytics.conviva.theolive

import android.util.Log
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.event.player.theolive.EndpointLoadedEvent
import com.theoplayer.android.api.event.player.theolive.IntentToFallbackEvent
import com.theoplayer.android.api.event.player.theolive.TheoLiveEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.connector.analytics.conviva.BuildConfig
import com.theoplayer.android.connector.analytics.conviva.ConvivaMetadata

private const val TAG = "THEOliveReporter"
private const val ENCODING_TYPE = "encoding_type"

class THEOliveReporter(val player: Player, val convivaVideoAnalytics: ConvivaVideoAnalytics) {
    init {
        addEventListeners()
    }

    fun destroy() {
        removeEventListeners()
    }

    private fun addEventListeners() {
        player.theoLive.addEventListener(TheoLiveEventTypes.ENDPOINTLOADED, this::onEndPointLoaded)
        player.theoLive.addEventListener(TheoLiveEventTypes.INTENTTOFALLBACK, this::onIntentToFallback)
    }

    private fun removeEventListeners() {
        player.theoLive.removeEventListener(TheoLiveEventTypes.ENDPOINTLOADED, this::onEndPointLoaded)
        player.theoLive.removeEventListener(TheoLiveEventTypes.INTENTTOFALLBACK, this::onIntentToFallback)
    }

    private fun onEndPointLoaded(event: EndpointLoadedEvent) {
        val endpoint = event.getEndpoint()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onEndPointLoaded - endpoint: $endpoint")
        }

        convivaVideoAnalytics.setContentInfo(
            mutableMapOf<String, String>().apply {
                // Report CDN
                endpoint.cdn?.let { cdn ->
                    put(ConvivaSdkConstants.DEFAULT_RESOURCE, cdn)
                }

                // Report encoding_type
                put(ENCODING_TYPE, if (endpoint.hespSrc == null) "HLS" else "HESP")
            } as ConvivaMetadata
        )
    }

    private fun onIntentToFallback(event: IntentToFallbackEvent) {
        val reason = event.reason ?: "NA"
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "IntentToFallbackEvent - reason: $reason")
        }
        convivaVideoAnalytics.reportPlaybackEvent(
            "intentToFallback",
            mutableMapOf<String, Any>().apply { put("reason", reason) }
        )
    }
}