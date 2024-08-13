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
    val uplynkDescriptionConverter: UplynkSsaiDescriptionConverter
) : ServerSideAdIntegrationHandler {

    private val player: Player
        get() = theoplayerView.player

    private val uplynkApi = UplynkApi()

    override suspend fun setSource(source: SourceDescription): SourceDescription {
        val uplynkSource = source.sources.find { it.ssai is UplynkSsaiDescription } ?: return source
        val ssaiDescription = uplynkSource.ssai as? UplynkSsaiDescription ?: return source
        val response = uplynkApi.preplay(uplynkDescriptionConverter.buildPreplayUrl(ssaiDescription))

        val newSource = source.replaceSources(source.sources.toMutableList().apply {
            remove(uplynkSource)
            add(0, uplynkSource.replaceSrc(response.playURL))
        })
        return newSource
    }
}
