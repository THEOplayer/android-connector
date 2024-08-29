package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionRegistry
import com.theoplayer.android.connector.uplynk.internal.UplynkAdIntegration
import com.theoplayer.android.connector.uplynk.internal.UplynkSsaiDescriptionConverter
import com.theoplayer.android.connector.uplynk.internal.UplynkEventDispatcher
import com.theoplayer.android.connector.uplynk.internal.network.UplynkApi
import com.theoplayer.android.connector.uplynk.network.UplynkAd
import com.theoplayer.android.connector.uplynk.network.UplynkAdBreak
import com.theoplayer.android.connector.uplynk.network.UplynkAds

internal const val TAG = "UplynkConnector"

/**
 * A connector for the Uplynk Media Platform.
 *
 * @param theoplayerView
 *   The THEOplayer view, which will be connected to the created connector.
 *
 * @see [Uplynk Media Platform](https://docs.edgecast.com/video/index.html)
 */
class UplynkConnector(
    private val theoplayerView: THEOplayerView,
) {
    private lateinit var integration: UplynkAdIntegration

    /**
     * Scheduled ad breaks
     */
    var adBreaks: List<UplynkAdBreak>? = null
        private set

    /**
     * The ad break that is currently played or `null` otherwise
     */
    var currentAdBreak: UplynkAdBreak? = null
        private set

    /**
     * The ad that is currently played or `null` otherwise
     */
    var currentAd: UplynkAd? = null
        private set

    private val eventDispatcher = UplynkEventDispatcher().also {
        it.addListener(object : UplynkListener {
            override fun onAdBreaksUpdated(ads: UplynkAds) {
                adBreaks = ads.breaks
            }

            override fun onAdBegin(ad: UplynkAd) {
                check(currentAd == null) { "Begin ad that before ending previous currentAd = ${currentAd} beginAd = $ad" }
                currentAd = ad
            }

            override fun onAdEnd(ad: UplynkAd) {
                check(currentAd == ad) { "Trying to end ad that is not current. currentAd = ${currentAd} endedAd = $ad" }
                currentAd = null
            }

            override fun onAdBreakBegin(adBreak: UplynkAdBreak) {
                check(currentAdBreak == null) { "Begin adbreak before ending previous currentAdBreak = ${currentAdBreak} beginAdBreak = $adBreak" }
                currentAdBreak = adBreak
            }

            override fun onAdBreakEnd(adBreak: UplynkAdBreak) {
                check(currentAdBreak == adBreak) { "Trying to end adbreak that is not current. currentAdBreak = ${currentAdBreak} endedAdBreak = $adBreak" }
                currentAdBreak = null
            }
        })
    }

    init {
        theoplayerView.player.ads.registerServerSideIntegration(INTEGRATION_ID, this::setupIntegration)
    }

    private fun setupIntegration(controller: ServerSideAdIntegrationController): UplynkAdIntegration {
        val integration = UplynkAdIntegration(
            theoplayerView,
            controller,
            eventDispatcher,
            UplynkSsaiDescriptionConverter(),
            UplynkApi()
        )
        this.integration = integration
        return integration
    }

    /**
     * Add a listener for events
     */
    fun addListener(listener: UplynkListener) = eventDispatcher.addListener(listener)

    /**
     * Remove a listener for events
     */
    fun removeListener(listener: UplynkListener) = eventDispatcher.removeListener(listener)

    companion object {
        /**
         * The integration identifier for the Uplynk connector.
         *
         * Ads created by this connector have this value as their [custom integration][Ad.getCustomIntegration].
         */
        const val INTEGRATION_ID = "uplynk"

        init {
            CustomSsaiDescriptionRegistry.register(INTEGRATION_ID, UplynkSsaiDeserializer())
        }
    }
}