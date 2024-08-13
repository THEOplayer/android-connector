package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import com.theoplayer.android.connector.uplynk.network.UplynkApi

internal class UplynkAdIntegration(
    val theoplayerView: THEOplayerView,
    val controller: ServerSideAdIntegrationController,
    val eventDispatcher: UplynkEventDispatcher,
    val uplynkDescriptionConverter: UplynkSsaiDescriptionConverter,
    private val uplynkApi: UplynkApi
) : ServerSideAdIntegrationHandler {

    private val player: Player
        get() = theoplayerView.player

    override suspend fun setSource(source: SourceDescription): SourceDescription {

        val uplynkSource = source.sources.singleOrNull { it.ssai is UplynkSsaiDescription }
        val ssaiDescription = uplynkSource?.ssai as? UplynkSsaiDescription ?: return source

        val response = uplynkDescriptionConverter
            .buildPreplayUrl(ssaiDescription)
            .let { uplynkApi.preplay(it) }
            .also {
                try {
                    eventDispatcher.dispatchPreplayEvents(it)
                } catch (e: Exception) {
                    controller.error(e)
                }
            }

        val internalResponse = response.parseMinimalResponse()

        val newSource = source.replaceSources(source.sources.toMutableList().apply {
            remove(uplynkSource)
            add(0, uplynkSource.replaceSrc(internalResponse.playURL))
        })
        if (ssaiDescription.assetInfo) {
            uplynkDescriptionConverter
                .buildAssetInfoUrls(ssaiDescription, internalResponse.sid)
                .mapNotNull {
                    try {
                        uplynkApi.assetInfo(it)
                    } catch (e: Exception) {
                        controller.error(e)
                        null
                    }
                }
                .forEach { eventDispatcher.dispatchAssetInfoEvents(it) }
        }

        return newSource
    }
}
