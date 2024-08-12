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
            .also { eventDispatcher.dispatchPreplayEvents(it) }


        val newSource = source.replaceSources(source.sources.toMutableList().apply {
            remove(uplynkSource)
            add(0, uplynkSource.replaceSrc(response.internalResponse.playURL))
        })
        if (ssaiDescription.assetInfo) {
            uplynkDescriptionConverter
                .buildAssetInfoUrls(ssaiDescription, response.internalResponse.sid)
                .map { uplynkApi.assetInfo(it) }
                .forEach { eventDispatcher.dispatchAssetInfoEvents(it) }
        }

        return newSource
    }
}
