package com.theoplayer.android.connector.analytics.conviva

import android.content.Context
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.THEOplayerGlobal
import com.theoplayer.android.api.event.player.DestroyEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.SourceChangeEvent
import com.theoplayer.android.api.player.Player

class ConvivaConnector {

    private val player: Player
    private val videoAnalytics: ConvivaVideoAnalytics
    private val adAnalytics: ConvivaAdAnalytics
    private val videoHandler: VideoHandler
    private val adHandler: AdHandler
    private var verizonMediaHandler: VerizonMediaHandler? = null

    @JvmOverloads
    constructor(appContext: Context, player: Player, customerKey: String, settings: Map<String, Any>? = null) {
        if (settings == null) {
            ConvivaAnalytics.init(appContext, customerKey)
        } else {
            ConvivaAnalytics.init(appContext, customerKey, settings)
        }

        this.player = player
        this.videoAnalytics = ConvivaAnalytics.buildVideoAnalytics(appContext)
        this.adAnalytics = ConvivaAnalytics.buildAdAnalytics(appContext, videoAnalytics)
        this.videoHandler = VideoHandler(player, videoAnalytics)
        this.adHandler = AdHandler(player, videoAnalytics, adAnalytics)

        player.verizonMedia?.let { verizonMedia ->
            this.verizonMediaHandler = VerizonMediaHandler(player, verizonMedia, videoAnalytics, adAnalytics)
        }

        setPlayerInfo()
        player.addEventListener(PlayerEventTypes.SOURCECHANGE, this::handleSourceChangeEvent)
        player.addEventListener(PlayerEventTypes.DESTROY, this::handleDestroyEvent)
    }

    fun setViewerId(viewerId: String) {
        val contentInfo = HashMap<String, Any>()
        contentInfo[ConvivaSdkConstants.VIEWER_ID] = viewerId
        setContentInfo(contentInfo)
    }

    fun setAssetName(assetName: String) {
        val contentInfo = HashMap<String, Any>()
        contentInfo[ConvivaSdkConstants.ASSET_NAME] = assetName
        setContentInfo(contentInfo)
    }

    fun setContentInfo(contentInfo: Map<String, Any>) {
        videoAnalytics.setContentInfo(contentInfo)
    }

    fun setAdInfo(adInfo: Map<String, Any>) {
        adAnalytics.setAdInfo(adInfo)
    }

    private fun setPlayerInfo() {
        val playerInfo = HashMap<String, Any>()
        playerInfo[ConvivaSdkConstants.FRAMEWORK_NAME] = "THEOplayer"
        playerInfo[ConvivaSdkConstants.FRAMEWORK_VERSION] = THEOplayerGlobal.getVersion()
        videoAnalytics.setPlayerInfo(playerInfo)
    }

    private fun handleSourceChangeEvent(sourceChangeEvent: SourceChangeEvent) {
        verizonMediaHandler?.reset()
        adHandler.reset()
        videoHandler.reset()
    }

    private fun handleDestroyEvent(destroyEvent: DestroyEvent) {
        destroy()
    }

    fun destroy() {
        player.removeEventListener(PlayerEventTypes.SOURCECHANGE, this::handleSourceChangeEvent)
        player.removeEventListener(PlayerEventTypes.DESTROY, this::handleDestroyEvent)
        verizonMediaHandler?.destroy()
        adHandler.destroy()
        videoHandler.destroy()
        adAnalytics.release()
        videoAnalytics.release()
        ConvivaAnalytics.release()
    }

}