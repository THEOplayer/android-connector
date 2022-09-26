package com.theoplayer.android.connector.analytics.conviva

import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.event.verizonmedia.*
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.verizonmedia.VerizonMedia
import com.theoplayer.android.api.verizonmedia.ads.VerizonMediaAdBreak

class VerizonMediaHandler {

    private val player: Player
    private val verizonMedia: VerizonMedia
    private val videoAnalytics: ConvivaVideoAnalytics
    private val adAnalytics: ConvivaAdAnalytics
    private var currentAdBreak: VerizonMediaAdBreak? = null
    private var adBreakCounter = 0

    constructor(player: Player, verizonMedia: VerizonMedia, videoAnalytics: ConvivaVideoAnalytics, adAnalytics: ConvivaAdAnalytics) {
        this.player = player
        this.verizonMedia = verizonMedia
        this.videoAnalytics = videoAnalytics
        this.adAnalytics = adAnalytics
        attachListeners()
    }

    private fun attachListeners() {
        player.addEventListener(PlayerEventTypes.PLAYING, this::handlePlayingEvent)
        player.addEventListener(PlayerEventTypes.PAUSE, this::handlePauseEvent)
        verizonMedia.ads.adBreaks.addEventListener(VerizonMediaAdBreakListEventTypes.ADD_ADBREAK, this::handleAddAdBreakEvent)
        verizonMedia.ads.adBreaks.addEventListener(VerizonMediaAdBreakListEventTypes.REMOVE_ADBREAK, this::handleRemoveAdBreakEvent)
    }

    private fun handlePlayingEvent(playingEvent: PlayingEvent) {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
    }

    private fun handlePauseEvent(pauseEvent: PauseEvent) {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PAUSED)
    }

    private fun handleAddAdBreakEvent(addAdBreakEvent: VerizonMediaAdBreakListEvent) {
        addAdBreakEvent.adBreak.addEventListener(VerizonMediaAdBreakEventTypes.ADBREAK_BEGIN, this::handleAdBreakBeginEvent)
        addAdBreakEvent.adBreak.addEventListener(VerizonMediaAdBreakEventTypes.ADBREAK_END, this::handleAdBreakEndEvent)
        addAdBreakEvent.adBreak.addEventListener(VerizonMediaAdBreakEventTypes.ADBREAK_SKIP, this::handleAdBreakSkipEvent)
        addAdBreakEvent.adBreak.ads?.let { ads ->
            ads.forEach { ad ->
                ad.addEventListener(VerizonMediaAdEventTypes.AD_BEGIN, this::handleAdBeginEvent)
                ad.addEventListener(VerizonMediaAdEventTypes.AD_END, this::handleAdEndEvent)
            }
        }
    }

    private fun handleAdBreakBeginEvent(adBreakBeginEvent: VerizonMediaAdBreakEvent) {
        currentAdBreak = adBreakBeginEvent.adBreak
        adBreakCounter++

        val adBreakInfo = HashMap<String, Any>()
        adBreakInfo[ConvivaSdkConstants.POD_INDEX] = adBreakCounter
        adBreakBeginEvent.adBreak.duration?.let { duration ->
            adBreakInfo[ConvivaSdkConstants.DURATION] = duration
        }

        videoAnalytics.reportAdBreakStarted(ConvivaSdkConstants.AdPlayer.CONTENT, ConvivaSdkConstants.AdType.SERVER_SIDE, adBreakInfo)
    }

    private fun handleAdBreakEndEvent(adBreakEndEvent: VerizonMediaAdBreakEvent) {
        videoAnalytics.reportAdBreakEnded()
        currentAdBreak = null
    }

    private fun handleAdBreakSkipEvent(adBreakSkipEvent: VerizonMediaAdBreakEvent) {
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.STOPPED)
        videoAnalytics.reportAdBreakEnded()
        currentAdBreak = null
    }

    private fun handleAdBeginEvent(adBeginEvent: VerizonMediaAdEvent) {
        val adInfo = HashMap<String, Any>()
        adInfo[ConvivaSdkConstants.ASSET_NAME] = adBeginEvent.ad.creative
        adInfo[ConvivaSdkConstants.DURATION] = adBeginEvent.ad.duration
        adInfo[ConvivaSdkConstants.IS_LIVE] =
            if (player.duration.isFinite()) {
                ConvivaSdkConstants.StreamType.VOD
            } else {
                ConvivaSdkConstants.StreamType.LIVE
            }

        adAnalytics.setAdInfo(adInfo)
        adAnalytics.reportAdStarted(adInfo)
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, adBeginEvent.ad.width, adBeginEvent.ad.height)

        player.videoTracks
            .first { it.isEnabled }
            .activeQuality?.let { videoQuality ->
                adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, videoQuality.bandwidth / 1000)
                adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE, videoQuality.frameRate)
            }
    }

    private fun handleAdEndEvent(adEndEvent: VerizonMediaAdEvent) {
        adAnalytics.reportAdEnded()
    }

    private fun handleRemoveAdBreakEvent(removeAdBreakEvent: VerizonMediaAdBreakListEvent) {
        removeAdBreakEventListeners(removeAdBreakEvent.adBreak)
    }

    private fun removeAdBreakEventListeners(adBreak: VerizonMediaAdBreak) {
        adBreak.removeEventListener(VerizonMediaAdBreakEventTypes.ADBREAK_BEGIN, this::handleAdBreakBeginEvent)
        adBreak.removeEventListener(VerizonMediaAdBreakEventTypes.ADBREAK_END, this::handleAdBreakEndEvent)
        adBreak.removeEventListener(VerizonMediaAdBreakEventTypes.ADBREAK_SKIP, this::handleAdBreakSkipEvent)
        adBreak.ads?.let { ads ->
            ads.forEach { ad ->
                ad.removeEventListener(VerizonMediaAdEventTypes.AD_BEGIN, this::handleAdBeginEvent)
                ad.removeEventListener(VerizonMediaAdEventTypes.AD_END, this::handleAdEndEvent)
            }
        }
    }

    private fun removeListeners() {
        player.removeEventListener(PlayerEventTypes.PLAYING, this::handlePlayingEvent)
        player.removeEventListener(PlayerEventTypes.PAUSE, this::handlePauseEvent)
        verizonMedia.ads.adBreaks.removeEventListener(VerizonMediaAdBreakListEventTypes.ADD_ADBREAK, this::handleAddAdBreakEvent)
        verizonMedia.ads.adBreaks.removeEventListener(VerizonMediaAdBreakListEventTypes.REMOVE_ADBREAK, this::handleRemoveAdBreakEvent)
        verizonMedia.ads.adBreaks.forEach { adBreak ->
            removeAdBreakEventListeners(adBreak)
        }
    }

    fun reset() {
        currentAdBreak = null
        adBreakCounter = 0
    }

    fun destroy() {
        // releasing of videoAnalytics & adAnalytics is done in ConvivaConnector
        removeListeners()
    }

}