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

    var adBreaks: List<UplynkAdBreak>? = null
        private set
    var currentAdBreak: UplynkAdBreak? = null
        private set
    var currentAd: UplynkAd? = null
        private set

    private val eventDispatcher = UplynkEventDispatcher().also {
        it.addListener(object : UplynkListener {
            override fun onAdBreaksUpdated(ads: UplynkAds) {
                this@UplynkConnector.adBreaks = ads.breaks
            }

            override fun onAdBegin(ad: UplynkAd) {
                check(this@UplynkConnector.currentAd == null) { "Begin ad that before ending previous currentAd = ${this@UplynkConnector.currentAd} beginAd = $ad" }
                this@UplynkConnector.currentAd = ad
            }

            override fun onAdEnd(ad: UplynkAd) {
                check(this@UplynkConnector.currentAd == ad) { "Trying to end ad that is not current. currentAd = ${this@UplynkConnector.currentAd} endedAd = $ad" }
                this@UplynkConnector.currentAd = null
            }

            override fun onAdBreakBegin(adBreak: UplynkAdBreak) {
                check(this@UplynkConnector.currentAdBreak == null) { "Begin adbreak before ending previous currentAdBreak = ${this@UplynkConnector.currentAdBreak} beginAdBreak = $adBreak" }
                this@UplynkConnector.currentAdBreak = adBreak
            }

            override fun onAdBreakEnd(adBreak: UplynkAdBreak) {
                check(this@UplynkConnector.currentAdBreak == adBreak) { "Trying to end adbreak that is not current. currentAdBreak = ${this@UplynkConnector.currentAdBreak} endedAdBreak = $adBreak" }
                this@UplynkConnector.currentAdBreak = null
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

    fun addListener(listener: UplynkListener) = eventDispatcher.addListener(listener)

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