package com.theoplayer.android.connector.uplynk

import android.util.Log
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.connector.uplynk.api.UplynkApi

class UplynkAdIntegration(
    private val theoplayerView: THEOplayerView,
    private val controller: ServerSideAdIntegrationController
): ServerSideAdIntegrationHandler {
    private val player: Player
        get() = theoplayerView.player
    private val uplynkApi = UplynkApi()
    override suspend fun setSource(source: SourceDescription): SourceDescription {
        Log.d("OlegSPY", "Set source ")
        val uplynkSource = source.sources.find { it.ssai is UplynkSsaiDescription } ?: return source
        val ssaiDescription = uplynkSource.ssai as? UplynkSsaiDescription ?: return source
        Log.d("OlegSPY", "Set source ${ssaiDescription.srcURL}")
        val response = uplynkApi.preplay(ssaiDescription.srcURL)
        Log.d("OlegSPY", "response json: " + response.playURL)

        return SourceDescription.Builder(response.playURL).build()
    }
}
