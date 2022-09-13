package com.theoplayer.android.connector.analytics.conviva

import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.player.Player

abstract class AdHandler {

    protected val player: Player
    protected val videoAnalytics: ConvivaVideoAnalytics
    protected val adAnalytics: ConvivaAdAnalytics

    private var adBreakCounter = 0
    private var currentAdBreak: AdBreak? = null

    constructor(player: Player, videoAnalytics: ConvivaVideoAnalytics, adAnalytics: ConvivaAdAnalytics) {
        this.player = player
        this.videoAnalytics = videoAnalytics
        this.adAnalytics = adAnalytics
    }

    protected abstract fun attachListeners()

    protected fun handlePlayEvent() {
        if (currentAdBreak == null) {
            return
        }
        // play/pause event would be triggered by the player but we need to let Conviva to know that we are indeed PlayerState.PLAYING after un-pausing.
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
    }

    protected fun handlePlayingEvent() {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
    }

    protected fun handlePauseEvent() {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PAUSED)
    }

    protected fun handleAdBreakBeginEvent(adBreak: AdBreak?) {
        if (adBreak == null) {
            return
        }

        currentAdBreak = adBreak
        adBreakCounter++

        val adBreakInfo = HashMap<String, Any>()
        adBreakInfo[ConvivaSdkConstants.POD_DURATION] = adBreak.maxDuration
        adBreakInfo[ConvivaSdkConstants.POD_INDEX] = adBreakCounter
        val timeOffset = adBreak.timeOffset
        when {
            timeOffset == 0 -> adBreakInfo[ConvivaSdkConstants.POD_POSITION] = ConvivaSdkConstants.AdPosition.PREROLL
            timeOffset > 0 -> adBreakInfo[ConvivaSdkConstants.POD_POSITION] = ConvivaSdkConstants.AdPosition.MIDROLL
            timeOffset < 0 -> adBreakInfo[ConvivaSdkConstants.POD_POSITION] = ConvivaSdkConstants.AdPosition.POSTROLL
        }

        videoAnalytics.reportAdBreakStarted(ConvivaSdkConstants.AdPlayer.CONTENT, ConvivaSdkConstants.AdType.CLIENT_SIDE, adBreakInfo)
    }

    protected fun handleAdBreakEndEvent() {
        videoAnalytics.reportAdBreakEnded()
        currentAdBreak = null
    }

    protected abstract fun handleAdBeginEvent(ad: Ad?)

    protected fun handleAdEndEvent(type: String?) {
        if (type != "linear") {
            return
        }
        adAnalytics.reportAdEnded()
    }

    protected fun handleAdSkipEvent() {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.STOPPED)
    }

    protected fun handleAdBufferingEvent() {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING)
    }

    protected fun handleAdErrorEvent(error: String?) {
        adAnalytics.reportAdFailed(error)
    }

    fun reset() {
        adBreakCounter = 0
        currentAdBreak = null
    }

    fun destroy() {
        // releasing of videoAnalytics & adAnalytics is done in ConvivaConnector
        removeListeners()
    }

    protected abstract fun removeListeners()

}
