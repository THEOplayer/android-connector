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
import com.theoplayer.android.connector.analytics.conviva.utils.contentProtectionConfigurationToMetadata
import com.theoplayer.android.connector.analytics.conviva.utils.flattenErrorObject

private const val TAG = "THEOliveReporter"

class THEOliveReporter(val player: Player, val convivaVideoAnalytics: ConvivaVideoAnalytics) {
    init {
        addEventListeners()
    }

    private fun onEndPointLoaded(event: EndpointLoadedEvent) {
        val endpoint = event.getEndpoint()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onEndPointLoaded - endpoint: $endpoint")
        }

        convivaVideoAnalytics.reportPlaybackEvent(
            "endPointLoaded",
            mutableMapOf<String, Any>().apply {
                put("endpoint", mutableMapOf<String, Any>().apply {
                    put("weight", endpoint.weight)
                    endpoint.cdn?.let { put("cdn", it) }
                    endpoint.hespSrc?.let { put("hespSrc", it) }
                    endpoint.hlsSrc?.let { put("hlsSrc", it) }
                    endpoint.adSrc?.let { put("adSrc", it) }
                    endpoint.targetLatency?.let { put("targetLatency", it) }
                    endpoint.daiAssetKey?.let { put("daiAssetKey", it) }
                    endpoint.contentProtection?.let {
                        put("contentProtection", contentProtectionConfigurationToMetadata(it))
                    }
                })
            }
        )

        convivaVideoAnalytics.setContentInfo(
            mutableMapOf<String, String>().apply {
                endpoint.cdn?.let { cdn ->
                    put(ConvivaSdkConstants.DEFAULT_RESOURCE, cdn)
                }
            } as ConvivaMetadata
        )
    }

    private fun onIntentToFallback(event: IntentToFallbackEvent) {
        val reason = event.reason
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "IntentToFallbackEvent - reason: ${reason ?: "NA"}")
        }
        convivaVideoAnalytics.reportPlaybackEvent(
            "intentToFallback",
            mutableMapOf<String, Any>().apply {
                reason?.let {
                    put("reason", flattenErrorObject(reason))
                }
            }
        )
    }

    private fun addEventListeners() {
        player.theoLive.addEventListener(TheoLiveEventTypes.ENDPOINTLOADED, this::onEndPointLoaded)
        player.theoLive.addEventListener(
            TheoLiveEventTypes.INTENTTOFALLBACK,
            this::onIntentToFallback
        )
    }

    private fun removeEventListeners() {
        player.theoLive.removeEventListener(
            TheoLiveEventTypes.ENDPOINTLOADED,
            this::onEndPointLoaded
        )
        player.theoLive.removeEventListener(
            TheoLiveEventTypes.INTENTTOFALLBACK,
            this::onIntentToFallback
        )
    }

    fun destroy() {
        removeEventListeners()
    }
}