package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionRegistry
import com.theoplayer.android.connector.uplynk.internal.UplynkAdIntegration
import com.theoplayer.android.connector.uplynk.internal.UplynkSsaiDescriptionConverter
import com.theoplayer.android.connector.uplynk.internal.UplynkEventDispatcher
import com.theoplayer.android.connector.uplynk.internal.UplynkSsaiDescriptionDeserializer
import com.theoplayer.android.connector.uplynk.internal.network.UplynkApi

internal const val TAG = "UplynkConnector"

/**
 * A connector for the Uplynk Platform.
 *
 * @param theoplayerView
 *   The THEOplayer view, which will be connected to the created connector.
 *
 * @see [Uplynk Platform](https://docs.edgecast.com/video/index.html)
 */
class UplynkConnector(
    private val theoplayerView: THEOplayerView,
    private val uplynkConfiguration: UplynkConfiguration
) {
    private lateinit var integration: UplynkAdIntegration
    private val eventDispatcher = UplynkEventDispatcher()

    init {
        theoplayerView.player.ads.registerServerSideIntegration(INTEGRATION_ID, this::setupIntegration)
    }

    private fun setupIntegration(controller: ServerSideAdIntegrationController): UplynkAdIntegration {
        val integration = UplynkAdIntegration(
            theoplayerView,
            controller,
            eventDispatcher,
            UplynkSsaiDescriptionConverter(),
            UplynkApi(),
            uplynkConfiguration
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
            CustomSsaiDescriptionRegistry.register(INTEGRATION_ID, UplynkSsaiDescriptionDeserializer)
        }
    }
}