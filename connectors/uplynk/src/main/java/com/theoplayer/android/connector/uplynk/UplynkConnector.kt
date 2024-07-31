package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionRegistry

internal const val TAG = "UplynkConnector"
    // TODO replace to buildconfig version
internal const val USER_AGENT = "THEOplayerUplynkConnector/buildconfigversion"

/**
 * A connector for the Uplynk Media Platform.
 *
 * @param theoplayerView
 *   The THEOplayer view, which will be connected to the created connector.
 */
class UplynkConnector(
    private val theoplayerView: THEOplayerView,
) {
    private lateinit var integration: UplynkAdIntegration

    init {
        theoplayerView.player.ads.registerServerSideIntegration(INTEGRATION_ID, this::setupIntegration)
    }

    private fun setupIntegration(controller: ServerSideAdIntegrationController): UplynkAdIntegration {
        val integration = UplynkAdIntegration(
            theoplayerView,
            controller,
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