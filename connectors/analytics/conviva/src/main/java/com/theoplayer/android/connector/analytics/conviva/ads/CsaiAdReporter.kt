package com.theoplayer.android.connector.analytics.conviva.ads

import android.util.Log
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaExperienceAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.GoogleImaAd
import com.theoplayer.android.api.ads.ima.GoogleImaAdEvent
import com.theoplayer.android.api.ads.ima.GoogleImaAdEventType
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.*
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.connector.analytics.conviva.BuildConfig
import com.theoplayer.android.connector.analytics.conviva.utils.calculateCurrentAdBreakInfo
import com.theoplayer.android.connector.analytics.conviva.utils.collectAdMetadata
import com.theoplayer.android.connector.analytics.conviva.utils.collectPlayerInfo

fun isAdLinear(ad: GoogleImaAd?): Boolean {
    return ad?.type == "linear"
}

const val TAG = "CsaiAdReporter"

@Suppress("UNUSED_PARAMETER", "SpellCheckingInspection")
class CsaiAdReporter(
    private val player: Player,
    private val convivaVideoAnalytics: ConvivaVideoAnalytics,
    private val convivaAdAnalytics: ConvivaAdAnalytics,
    private val maybeReportPlaybackRequested: () -> Unit
) : ConvivaExperienceAnalytics.ICallback {
    private var currentAdBreak: AdBreak? = null
    private var currentAd: GoogleImaAd? = null
    private var adBreakCounter = 0

    private val onPlay: EventListener<PlayEvent>
    private val onPlaying: EventListener<PlayingEvent>
    private val onPause: EventListener<PauseEvent>

    private val onAdStarted: EventListener<GoogleImaAdEvent>
    private val onAdCompleted: EventListener<GoogleImaAdEvent>
    private val onAdSkip: EventListener<GoogleImaAdEvent>
    private val onAdBuffering: EventListener<GoogleImaAdEvent>
    private val onAdError: EventListener<GoogleImaAdEvent>
    private val onContentResume: EventListener<GoogleImaAdEvent>

    init {
        convivaAdAnalytics.setCallback(this)
        convivaAdAnalytics.setAdPlayerInfo(collectPlayerInfo())

        onPlay = EventListener<PlayEvent> {
            if (currentAdBreak != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "reportAdMetric - PlayerState.PLAYING")
                }
                convivaAdAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.PLAYING
                )
            }
        }

        onPlaying = EventListener<PlayingEvent> {
            if (currentAdBreak != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "reportAdMetric - PlayerState.PLAYING")
                }
                convivaAdAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.PLAYING
                )
            }
        }

        onPause = EventListener<PauseEvent> {
            if (currentAdBreak != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "reportAdMetric - PlayerState.PAUSED")
                }
                convivaAdAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.PAUSED
                )
            }
        }

        onAdStarted = EventListener<GoogleImaAdEvent> { event ->
            handleAdBegin(event.ad)
        }

        onAdCompleted = EventListener<GoogleImaAdEvent> { event ->
            handleAdEnd(event.ad)
        }

        onAdSkip = EventListener<GoogleImaAdEvent> {
            if (currentAdBreak != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "reportAdMetric - PlayerState.STOPPED")
                }
                convivaAdAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.STOPPED
                )
            }
        }

        onAdBuffering = EventListener<GoogleImaAdEvent> {
            if (currentAdBreak != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "reportAdMetric - PlayerState.BUFFERING")
                }
                convivaAdAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.BUFFERING
                )
            }
        }

        onAdError = EventListener<GoogleImaAdEvent> {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "reportAdFailed")
            }
            convivaAdAnalytics.reportAdFailed("Ad Request Failed")
        }

        onContentResume = EventListener<GoogleImaAdEvent> {
            handleAdBreakEnd()
        }

        addEventListeners()
    }

    // Update API will be called by Conviva SDK at regular intervals to compute playback
    // metrics. This update callback will be called at the frequency of 1sec
    override fun update() {
        if (currentAdBreak == null) {
            return
        }
        convivaAdAnalytics.reportAdMetric(
            ConvivaSdkConstants.PLAYBACK.PLAY_HEAD_TIME,
            (1e3 * player.currentTime).toLong()
        )
    }

    override fun update(str: String?) {
        // unused
    }

    private fun addEventListeners() {
        player.addEventListener(PlayerEventTypes.PLAY, onPlay)
        player.addEventListener(PlayerEventTypes.PLAYING, onPlaying)
        player.addEventListener(PlayerEventTypes.PAUSE, onPause)

        player.ads.addEventListener(GoogleImaAdEventType.STARTED, onAdStarted)
        player.ads.addEventListener(GoogleImaAdEventType.COMPLETED, onAdCompleted)
        player.ads.addEventListener(GoogleImaAdEventType.SKIPPED, onAdSkip)
        player.ads.addEventListener(GoogleImaAdEventType.AD_BUFFERING, onAdBuffering)
        player.ads.addEventListener(GoogleImaAdEventType.AD_ERROR, onAdError)
        player.ads.addEventListener(GoogleImaAdEventType.CONTENT_RESUME_REQUESTED, onContentResume)
    }

    private fun removeEventListeners() {
        player.removeEventListener(PlayerEventTypes.PLAY, onPlay)
        player.removeEventListener(PlayerEventTypes.PLAYING, onPlaying)
        player.removeEventListener(PlayerEventTypes.PAUSE, onPause)

        player.ads.removeEventListener(GoogleImaAdEventType.STARTED, onAdStarted)
        player.ads.removeEventListener(GoogleImaAdEventType.COMPLETED, onAdCompleted)
        player.ads.removeEventListener(GoogleImaAdEventType.SKIPPED, onAdSkip)
        player.ads.removeEventListener(GoogleImaAdEventType.AD_BUFFERING, onAdBuffering)
        player.ads.removeEventListener(GoogleImaAdEventType.AD_ERROR, onAdError)
        player.ads.removeEventListener(GoogleImaAdEventType.CONTENT_RESUME_REQUESTED, onContentResume)
    }

    private fun handleAdBreakBegin(adBreak: AdBreak?, isLinearAdBreak: Boolean) {
        // Make sure the session is started
        maybeReportPlaybackRequested()

        if (this.currentAdBreak != null) {
            // AdBreak was already set, nothing needs to happen
            return
        }
        this.currentAdBreak = adBreak
        if (isLinearAdBreak && adBreak != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "reportAdBreakStarted - $adBreak")
            }
            adBreakCounter++
            convivaVideoAnalytics.reportAdBreakStarted(
                ConvivaSdkConstants.AdPlayer.CONTENT,
                ConvivaSdkConstants.AdType.CLIENT_SIDE,
                calculateCurrentAdBreakInfo(adBreak, adBreakCounter)
            )
        } else {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "handleAdBreakBegin - No valid adBreak")
            }
        }
    }

    private fun handleAdBegin(ad: GoogleImaAd?) {
        if (currentAdBreak == null && ad?.imaAd != null) {
            handleAdBreakBegin(ad.adBreak, isAdLinear(ad))
        }
        if (ad != null && isAdLinear(ad)) {
            currentAd = ad
            val adMetadata = collectAdMetadata(ad) +
                    mapOf("c3.csid" to convivaVideoAnalytics.sessionId.toString())
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "reportAdStarted - $adMetadata")
            }
            convivaAdAnalytics.setAdInfo(adMetadata)
            convivaAdAnalytics.reportAdLoaded(adMetadata)
            convivaAdAnalytics.reportAdStarted(adMetadata)
            convivaAdAnalytics.reportAdMetric(
                ConvivaSdkConstants.PLAYBACK.RESOLUTION,
                player.videoWidth,
                player.videoHeight
            )
            convivaAdAnalytics.reportAdMetric(
                ConvivaSdkConstants.PLAYBACK.BITRATE,
                player.videoWidth,
                ad.imaAd.vastMediaBitrate
            )
        } else {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "handleAdEnd - No valid ad")
            }
        }
    }

    private fun handleAdEnd(ad: GoogleImaAd?) {
        if (ad != null && isAdLinear(ad)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "reportAdEnded")
            }
            convivaAdAnalytics.reportAdEnded()
        } else {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "handleAdEnd - No current ad")
            }
        }
        currentAd = null
    }

    private fun handleAdBreakEnd() {
        if (this.currentAdBreak != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "reportAdBreakEnded")
            }
            convivaVideoAnalytics.reportAdBreakEnded()
            currentAdBreak = null
        } else {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "handleAdBreakEnd - No current adBreak")
            }
        }
        currentAdBreak = null
    }

    fun reset() {
        // Optionally report end of current Ad
        if (currentAd != null) {
            convivaAdAnalytics.reportAdEnded()
        }
        // Optionally report end of current AdBreak
        if (currentAdBreak != null) {
            convivaVideoAnalytics.reportAdBreakEnded()
        }
        currentAd = null
        currentAdBreak = null
        adBreakCounter = 0
    }

    fun destroy() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "destroy")
        }
        removeEventListeners()
    }
}
