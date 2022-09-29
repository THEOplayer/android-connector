package com.theoplayer.android.connector.analytics.conviva

import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.GoogleImaAd
import com.theoplayer.android.api.ads.ima.GoogleImaAdEvent
import com.theoplayer.android.api.ads.ima.GoogleImaAdEventType
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.player.Player

class UnifiedAdHandler : AdHandler {

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

    constructor(player: Player, videoAnalytics: ConvivaVideoAnalytics, adAnalytics: ConvivaAdAnalytics) : super(player, videoAnalytics, adAnalytics) {
        attachListeners()
    }

    override fun attachListeners() {
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

    override fun handleAdBeginEvent(ad: Ad?) {
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

    override fun removeListeners() {
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
