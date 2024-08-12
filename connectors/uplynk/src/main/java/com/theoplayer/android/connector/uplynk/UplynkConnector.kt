package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionRegistry
import com.theoplayer.android.connector.uplynk.internal.UplynkAdIntegration
import com.theoplayer.android.connector.uplynk.internal.UplynkSsaiDescriptionConverter
import com.theoplayer.android.connector.uplynk.common.EventDispatcherImpl
import com.theoplayer.android.connector.uplynk.internal.UplynkEventDispatcher
import com.theoplayer.android.connector.uplynk.network.UplynkApi

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
    val eventDispatcher = EventDispatcherImpl()

    init {
        theoplayerView.player.ads.registerServerSideIntegration(INTEGRATION_ID, this::setupIntegration)
    }

    private fun setupIntegration(controller: ServerSideAdIntegrationController): UplynkAdIntegration {
        val integration = UplynkAdIntegration(
            theoplayerView,
            controller,
            UplynkEventDispatcher(eventDispatcher),
            UplynkSsaiDescriptionConverter(),
            UplynkApi()
        )
        this.integration = integration
        return integration
    }

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