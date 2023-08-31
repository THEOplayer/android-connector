package com.theoplayer.android.connector.analytics.conviva

import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.event.verizonmedia.*
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.verizonmedia.VerizonMedia
import com.theoplayer.android.api.verizonmedia.ads.VerizonMediaAdBreak

class VerizonMediaHandler(
    private val player: Player,
    private val verizonMedia: VerizonMedia,
    private val videoAnalytics: ConvivaVideoAnalytics,
    private val adAnalytics: ConvivaAdAnalytics
) {

    private var currentAdBreak: VerizonMediaAdBreak? = null
    private var adBreakCounter = 0

    private val onPlaying: EventListener<PlayingEvent>
    private val onPause: EventListener<PauseEvent>
    private val onAddAdBreak: EventListener<VerizonMediaAdBreakListEvent>
    private val onRemoveAdBreak: EventListener<VerizonMediaAdBreakListEvent>
    private val onAdBreakBegin: EventListener<VerizonMediaAdBreakEvent>
    private val onAdBreakEnd: EventListener<VerizonMediaAdBreakEvent>
    private val onAdBreakSkip: EventListener<VerizonMediaAdBreakEvent>
    private val onAdBegin: EventListener<VerizonMediaAdEvent>
    private val onAdEnd: EventListener<VerizonMediaAdEvent>

    init {
        onPlaying = EventListener<PlayingEvent> {
            reportPlaying()
        }
        onPause = EventListener<PauseEvent> {
            reportPause()
        }
        onAddAdBreak = EventListener<VerizonMediaAdBreakListEvent> { event ->
            reportAddAdBreak(event)
        }
        onRemoveAdBreak = EventListener<VerizonMediaAdBreakListEvent> { event ->
            reportRemoveAdBreak(event)
        }
        onAdBreakBegin = EventListener<VerizonMediaAdBreakEvent> { event ->
            reportAdBreakBegin(event)
        }
        onAdBreakEnd = EventListener<VerizonMediaAdBreakEvent> {
            reportAdBreakEnd()
        }
        onAdBreakSkip = EventListener<VerizonMediaAdBreakEvent> {
            reportAdBreakSkip()
        }
        onAdBegin = EventListener<VerizonMediaAdEvent> { event ->
            reportAdBegin(event)
        }
        onAdEnd = EventListener<VerizonMediaAdEvent> {
            reportAdEnd()
        }
    }


    private fun reportPlaying() {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(
            ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
            ConvivaSdkConstants.PlayerState.PLAYING
        )
    }

    private fun reportPause() {
        if (currentAdBreak == null) {
            return
        }
        adAnalytics.reportAdMetric(
            ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
            ConvivaSdkConstants.PlayerState.PAUSED
        )
    }

    private fun reportAddAdBreak(addAdBreakEvent: VerizonMediaAdBreakListEvent) {
        addAdBreakEvent.adBreak.addEventListener(
            VerizonMediaAdBreakEventTypes.ADBREAK_BEGIN,
            onAdBreakBegin
        )
        addAdBreakEvent.adBreak.addEventListener(
            VerizonMediaAdBreakEventTypes.ADBREAK_END,
            onAdBreakEnd
        )
        addAdBreakEvent.adBreak.addEventListener(
            VerizonMediaAdBreakEventTypes.ADBREAK_SKIP,
            onAdBreakSkip
        )
        addAdBreakEvent.adBreak.ads?.let { ads ->
            ads.forEach { ad ->
                ad.addEventListener(VerizonMediaAdEventTypes.AD_BEGIN, onAdBegin)
                ad.addEventListener(VerizonMediaAdEventTypes.AD_END, onAdEnd)
            }
        }
    }

    private fun reportAdBreakBegin(adBreakBeginEvent: VerizonMediaAdBreakEvent) {
        currentAdBreak = adBreakBeginEvent.adBreak
        adBreakCounter++

        val adBreakInfo = HashMap<String, Any>()
        adBreakInfo[ConvivaSdkConstants.POD_INDEX] = adBreakCounter
        adBreakBeginEvent.adBreak.duration?.let { duration ->
            adBreakInfo[ConvivaSdkConstants.DURATION] = duration
        }

        videoAnalytics.reportAdBreakStarted(
            ConvivaSdkConstants.AdPlayer.CONTENT,
            ConvivaSdkConstants.AdType.SERVER_SIDE,
            adBreakInfo
        )
    }

    private fun reportAdBreakEnd() {
        videoAnalytics.reportAdBreakEnded()
        currentAdBreak = null
    }

    private fun reportAdBreakSkip() {
        adAnalytics.reportAdMetric(
            ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
            ConvivaSdkConstants.PlayerState.STOPPED
        )
        videoAnalytics.reportAdBreakEnded()
        currentAdBreak = null
    }

    private fun reportAdBegin(adBeginEvent: VerizonMediaAdEvent) {
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
        adAnalytics.reportAdMetric(
            ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
            ConvivaSdkConstants.PlayerState.PLAYING
        )
        adAnalytics.reportAdMetric(
            ConvivaSdkConstants.PLAYBACK.RESOLUTION,
            adBeginEvent.ad.width,
            adBeginEvent.ad.height
        )

        player.videoTracks
            .first { it.isEnabled }
            .activeQuality?.let { videoQuality ->
                adAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.BITRATE,
                    videoQuality.bandwidth / 1000
                )
                adAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE,
                    videoQuality.frameRate
                )
            }
    }

    private fun reportAdEnd() {
        adAnalytics.reportAdEnded()
    }

    private fun reportRemoveAdBreak(removeAdBreakEvent: VerizonMediaAdBreakListEvent) {
        removeAdBreakEventListeners(removeAdBreakEvent.adBreak)
    }

    private fun removeAdBreakEventListeners(adBreak: VerizonMediaAdBreak) {
        adBreak.removeEventListener(
            VerizonMediaAdBreakEventTypes.ADBREAK_BEGIN,
            onAdBreakBegin
        )
        adBreak.removeEventListener(
            VerizonMediaAdBreakEventTypes.ADBREAK_END,
            onAdBreakEnd
        )
        adBreak.removeEventListener(
            VerizonMediaAdBreakEventTypes.ADBREAK_SKIP,
            onAdBreakSkip
        )
        adBreak.ads?.let { ads ->
            ads.forEach { ad ->
                ad.removeEventListener(VerizonMediaAdEventTypes.AD_BEGIN, onAdBegin)
                ad.removeEventListener(VerizonMediaAdEventTypes.AD_END, onAdEnd)
            }
        }
    }

    private fun removeListeners() {
        player.removeEventListener(PlayerEventTypes.PLAYING, onPlaying)
        player.removeEventListener(PlayerEventTypes.PAUSE, onPause)
        verizonMedia.ads.adBreaks.removeEventListener(
            VerizonMediaAdBreakListEventTypes.ADD_ADBREAK,
            onAddAdBreak
        )
        verizonMedia.ads.adBreaks.removeEventListener(
            VerizonMediaAdBreakListEventTypes.REMOVE_ADBREAK,
            onRemoveAdBreak
        )
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