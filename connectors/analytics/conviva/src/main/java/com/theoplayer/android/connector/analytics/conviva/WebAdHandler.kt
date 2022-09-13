package com.theoplayer.android.connector.analytics.conviva

import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.LinearAd
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.ads.*
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.player.Player

class WebAdHandler : AdHandler {

    private val playEventListener = EventListener<PlayEvent> { handlePlayEvent() }
    private val playingEventListener = EventListener<PlayingEvent> { handlePlayingEvent() }
    private val pauseEventListener = EventListener<PauseEvent> { handlePauseEvent() }
    private val adBreakBeginEventListener = EventListener<AdBreakBeginEvent> { handleAdBreakBeginEvent(it.adBreak) }
    private val adBreakEndEventListener = EventListener<AdBreakEndEvent> { handleAdBreakEndEvent() }
    private val adBeginEventListener = EventListener<AdBeginEvent> { handleAdBeginEvent(it.ad) }
    private val adEndEventListener = EventListener<AdEndEvent> { handleAdEndEvent(it.ad?.type) }
    private val adErrorEventListener = EventListener<AdErrorEvent> { handleAdErrorEvent(it.error) }

    constructor(player: Player, videoAnalytics: ConvivaVideoAnalytics, adAnalytics: ConvivaAdAnalytics) : super(player, videoAnalytics, adAnalytics) {
        attachListeners()
    }

    override fun attachListeners() {
        player.addEventListener(PlayerEventTypes.PLAY, playEventListener)
        player.addEventListener(PlayerEventTypes.PLAYING, playingEventListener)
        player.addEventListener(PlayerEventTypes.PAUSE, pauseEventListener)
        player.ads.addEventListener(AdsEventTypes.AD_BREAK_BEGIN, adBreakBeginEventListener)
        player.ads.addEventListener(AdsEventTypes.AD_BREAK_END, adBreakEndEventListener)
        player.ads.addEventListener(AdsEventTypes.AD_BEGIN, adBeginEventListener)
        player.ads.addEventListener(AdsEventTypes.AD_END, adEndEventListener)
        player.ads.addEventListener(AdsEventTypes.AD_ERROR, adErrorEventListener)
    }

    override fun handleAdBeginEvent(ad: Ad?) {
        if (ad !is LinearAd) {
            return
        }

        val adInfo = HashMap<String, Any>()
        adInfo[ConvivaSdkConstants.ASSET_NAME] = ad.id
        adInfo[ConvivaSdkConstants.STREAM_URL] = ad.mediaFiles[0].resourceURI
        adInfo[ConvivaSdkConstants.DURATION] = ad.duration
        adInfo[ConvivaSdkConstants.IS_LIVE] =
            if (player.duration.isFinite()) {
                ConvivaSdkConstants.StreamType.VOD
            } else {
                ConvivaSdkConstants.StreamType.LIVE
            }

        adAnalytics.setAdInfo(adInfo)
        adAnalytics.reportAdLoaded(adInfo)
        adAnalytics.reportAdStarted(adInfo)
    }

    override fun removeListeners() {
        player.removeEventListener(PlayerEventTypes.PLAY, playEventListener)
        player.removeEventListener(PlayerEventTypes.PLAYING, playingEventListener)
        player.removeEventListener(PlayerEventTypes.PAUSE, pauseEventListener)
        player.ads.removeEventListener(AdsEventTypes.AD_BREAK_BEGIN, adBreakBeginEventListener)
        player.ads.removeEventListener(AdsEventTypes.AD_BREAK_END, adBreakEndEventListener)
        player.ads.removeEventListener(AdsEventTypes.AD_BEGIN, adBeginEventListener)
        player.ads.removeEventListener(AdsEventTypes.AD_END, adEndEventListener)
        player.ads.removeEventListener(AdsEventTypes.AD_ERROR, adErrorEventListener)
    }

}