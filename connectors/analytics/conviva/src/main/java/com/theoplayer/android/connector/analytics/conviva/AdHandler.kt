package com.theoplayer.android.connector.analytics.conviva

import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.GoogleImaAd
import com.theoplayer.android.api.ads.ima.GoogleImaAdEvent
import com.theoplayer.android.api.ads.ima.GoogleImaAdEventType
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.player.Player

class AdHandler {

    private val player: Player
    private val videoAnalytics: ConvivaVideoAnalytics
    private val adAnalytics: ConvivaAdAnalytics
    private val playEventListener = EventListener<PlayEvent> { handlePlayEvent() }
    private val playingEventListener = EventListener<PlayingEvent> { handlePlayingEvent() }
    private val pauseEventListener = EventListener<PauseEvent> { handlePauseEvent() }
    private val adBreakStartedEventListener = EventListener<GoogleImaAdEvent> { handleAdBreakBeginEvent(it.ad?.adBreak) }
    private val adBreakEndedEventListener = EventListener<GoogleImaAdEvent> { handleAdBreakEndEvent() }
    private val adStartedEventListener = EventListener<GoogleImaAdEvent> { handleAdBeginEvent(it.ad) }
    private val adCompletedEventListener = EventListener<GoogleImaAdEvent> { handleAdEndEvent(it.ad?.type) }
    private val adErrorEventListener = EventListener<GoogleImaAdEvent> { handleAdErrorEvent(null) }
    private val adSkippedEventListener = EventListener<GoogleImaAdEvent> { handleAdSkipEvent() }
    private val adBufferingEventListener = EventListener<GoogleImaAdEvent> { handleAdBufferingEvent() }

    private var adBreakCounter = 0
    private var currentAdBreak: AdBreak? = null

    constructor(player: Player, videoAnalytics: ConvivaVideoAnalytics, adAnalytics: ConvivaAdAnalytics) {
        this.player = player
        this.videoAnalytics = videoAnalytics
        this.adAnalytics = adAnalytics
        attachListeners()
    }

    private fun attachListeners() {
        player.addEventListener(PlayerEventTypes.PLAY, playEventListener)
        player.addEventListener(PlayerEventTypes.PLAYING, playingEventListener)
        player.addEventListener(PlayerEventTypes.PAUSE, pauseEventListener)
        player.ads.addEventListener(GoogleImaAdEventType.AD_BREAK_STARTED, adBreakStartedEventListener)
        player.ads.addEventListener(GoogleImaAdEventType.AD_BREAK_ENDED, adBreakEndedEventListener)
        player.ads.addEventListener(GoogleImaAdEventType.STARTED, adStartedEventListener)
        player.ads.addEventListener(GoogleImaAdEventType.COMPLETED, adCompletedEventListener)
        player.ads.addEventListener(GoogleImaAdEventType.SKIPPED, adSkippedEventListener)
        player.ads.addEventListener(GoogleImaAdEventType.AD_BUFFERING, adBufferingEventListener)
        player.ads.addEventListener(GoogleImaAdEventType.AD_ERROR, adErrorEventListener)
    }

    private fun handlePlayEvent() {
        if (currentAdBreak == null) {
            return
        }
        // play/pause event would be triggered by the player but we need to let Conviva to know that we are indeed PlayerState.PLAYING after un-pausing.
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
    }

    private fun handlePlayingEvent() {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
    }

    private fun handlePauseEvent() {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PAUSED)
    }

    private fun handleAdBreakBeginEvent(adBreak: AdBreak?) {
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

    private fun handleAdBreakEndEvent() {
        videoAnalytics.reportAdBreakEnded()
        currentAdBreak = null
    }

    private fun handleAdBeginEvent(ad: Ad?) {
        if (ad !is GoogleImaAd || ad.type != "linear") {
            return
        }

        val adInfo = HashMap<String, Any>()
        adInfo[ConvivaSdkConstants.ASSET_NAME] = ad.imaAd.title
        adInfo[ConvivaSdkConstants.STREAM_URL] = ad.imaAd.adId
        adInfo[ConvivaSdkConstants.DURATION] = ad.imaAd.duration
        adInfo[ConvivaSdkConstants.IS_LIVE] =
            if (player.duration.isFinite()) {
                ConvivaSdkConstants.StreamType.VOD
            } else {
                ConvivaSdkConstants.StreamType.LIVE
            }


        adAnalytics.setAdInfo(adInfo)
        adAnalytics.reportAdLoaded(adInfo)
        adAnalytics.reportAdStarted(adInfo)
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, player.videoWidth, player.videoHeight)
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, player.videoWidth, ad.imaAd.vastMediaBitrate)
    }

    private fun handleAdEndEvent(type: String?) {
        if (type != "linear") {
            return
        }
        adAnalytics.reportAdEnded()
    }

    private fun handleAdSkipEvent() {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.STOPPED)
    }

    private fun handleAdBufferingEvent() {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING)
    }

    private fun handleAdErrorEvent(error: String?) {
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

    private fun removeListeners() {
        player.removeEventListener(PlayerEventTypes.PLAY, playEventListener)
        player.removeEventListener(PlayerEventTypes.PLAYING, playingEventListener)
        player.removeEventListener(PlayerEventTypes.PAUSE, pauseEventListener)
        player.ads.removeEventListener(GoogleImaAdEventType.AD_BREAK_STARTED, adBreakStartedEventListener)
        player.ads.removeEventListener(GoogleImaAdEventType.AD_BREAK_ENDED, adBreakEndedEventListener)
        player.ads.removeEventListener(GoogleImaAdEventType.STARTED, adStartedEventListener)
        player.ads.removeEventListener(GoogleImaAdEventType.COMPLETED, adCompletedEventListener)
        player.ads.removeEventListener(GoogleImaAdEventType.SKIPPED, adSkippedEventListener)
        player.ads.removeEventListener(GoogleImaAdEventType.AD_BUFFERING, adBufferingEventListener)
        player.ads.removeEventListener(GoogleImaAdEventType.AD_ERROR, adErrorEventListener)
    }

}
