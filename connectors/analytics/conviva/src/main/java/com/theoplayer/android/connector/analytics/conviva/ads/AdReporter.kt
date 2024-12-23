package com.theoplayer.android.connector.analytics.conviva.ads

import android.util.Log
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaExperienceAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.GoogleImaAd
import com.theoplayer.android.api.ads.ima.GoogleImaAdEvent
import com.theoplayer.android.api.ads.ima.GoogleImaAdEventType
import com.theoplayer.android.api.event.EventDispatcher
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.ads.AdBeginEvent
import com.theoplayer.android.api.event.ads.AdBreakEndEvent
import com.theoplayer.android.api.event.ads.AdEndEvent
import com.theoplayer.android.api.event.ads.AdErrorEvent
import com.theoplayer.android.api.event.ads.AdEvent
import com.theoplayer.android.api.event.ads.AdSkipEvent
import com.theoplayer.android.api.event.ads.AdsEventTypes
import com.theoplayer.android.api.event.player.*
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.connector.analytics.conviva.BuildConfig
import com.theoplayer.android.connector.analytics.conviva.ConvivaHandlerBase
import com.theoplayer.android.connector.analytics.conviva.utils.calculateAdType
import com.theoplayer.android.connector.analytics.conviva.utils.calculateAdTypeAsString
import com.theoplayer.android.connector.analytics.conviva.utils.calculateCurrentAdBreakInfo
import com.theoplayer.android.connector.analytics.conviva.utils.collectAdMetadata
import com.theoplayer.android.connector.analytics.conviva.utils.collectPlayerInfo
import com.theoplayer.android.connector.analytics.conviva.utils.updateAdMetadataForGoogleIma

fun isAdLinear(ad: Ad?): Boolean {
    return ad?.type == "linear"
}

private const val TAG = "AdReporter"

@Suppress("SpellCheckingInspection")
class AdReporter(
    private val player: Player,
    private val convivaVideoAnalytics: ConvivaVideoAnalytics,
    private val convivaAdAnalytics: ConvivaAdAnalytics,
    private val convivaHandler: ConvivaHandlerBase,
    private val adEventsExtension: EventDispatcher<AdEvent<*>>?
) : ConvivaExperienceAnalytics.ICallback {
    private var currentAdBreak: AdBreak? = null
    private var currentAd: Ad? = null
    private var adBreakCounter = 0

    private val onPlay: EventListener<PlayEvent>
    private val onPlaying: EventListener<PlayingEvent>
    private val onPause: EventListener<PauseEvent>

    private val onImaAdStarted: EventListener<GoogleImaAdEvent>
    private val onImaAdCompleted: EventListener<GoogleImaAdEvent>
    private val onImaAdSkip: EventListener<GoogleImaAdEvent>
    private val onImaAdBuffering: EventListener<GoogleImaAdEvent>
    private val onImaAdError: EventListener<GoogleImaAdEvent>
    private val onImaContentResume: EventListener<GoogleImaAdEvent>

    private val onAdBegin: EventListener<AdBeginEvent>
    private val onAdEnd: EventListener<AdEndEvent>
    private val onAdBreakEnd: EventListener<AdBreakEndEvent>
    private val onAdSkip: EventListener<AdSkipEvent>
    private val onAdError: EventListener<AdErrorEvent>

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

        onImaAdStarted = EventListener<GoogleImaAdEvent> { event ->
            handleAdBegin(event.ad)
        }

        onImaAdCompleted = EventListener<GoogleImaAdEvent> { event ->
            handleAdEnd(event.ad)
        }

        onImaAdSkip = EventListener<GoogleImaAdEvent> {
            handleAdSkip()
        }

        onImaAdBuffering = EventListener<GoogleImaAdEvent> {
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

        onImaAdError = EventListener<GoogleImaAdEvent> {
            handleAdError()
        }

        onImaContentResume = EventListener<GoogleImaAdEvent> {
            handleAdBreakEnd()
        }

        onAdBegin = EventListener<AdBeginEvent> { event ->
            handleAdBegin(event.ad)
        }

        onAdEnd = EventListener<AdEndEvent> { event ->
            handleAdEnd(event.ad)
        }

        onAdBreakEnd = EventListener<AdBreakEndEvent> {
            handleAdBreakEnd()
        }

        onAdSkip = EventListener<AdSkipEvent> {
            handleAdSkip()
        }

        onAdError = EventListener<AdErrorEvent> {
            handleAdError()
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

        (listOf(player.ads, adEventsExtension)).forEach { ads ->
            ads?.addEventListener(AdsEventTypes.AD_BEGIN, onAdBegin)
            ads?.addEventListener(AdsEventTypes.AD_END, onAdEnd)
            ads?.addEventListener(AdsEventTypes.AD_BREAK_END, onAdBreakEnd)
            ads?.addEventListener(AdsEventTypes.AD_SKIP, onAdSkip)
            ads?.addEventListener(AdsEventTypes.AD_ERROR, onAdError)

            ads?.addEventListener(GoogleImaAdEventType.STARTED, onImaAdStarted)
            ads?.addEventListener(GoogleImaAdEventType.COMPLETED, onImaAdCompleted)
            ads?.addEventListener(GoogleImaAdEventType.SKIPPED, onImaAdSkip)
            ads?.addEventListener(GoogleImaAdEventType.AD_BUFFERING, onImaAdBuffering)
            ads?.addEventListener(GoogleImaAdEventType.AD_ERROR, onImaAdError)
            ads?.addEventListener(
                GoogleImaAdEventType.CONTENT_RESUME_REQUESTED,
                onImaContentResume
            )
        }
    }

    private fun removeEventListeners() {
        player.removeEventListener(PlayerEventTypes.PLAY, onPlay)
        player.removeEventListener(PlayerEventTypes.PLAYING, onPlaying)
        player.removeEventListener(PlayerEventTypes.PAUSE, onPause)

        (listOf(player.ads, adEventsExtension)).forEach { ads ->
            ads?.addEventListener(AdsEventTypes.AD_BEGIN, onAdBegin)
            ads?.addEventListener(AdsEventTypes.AD_END, onAdEnd)
            ads?.addEventListener(AdsEventTypes.AD_BREAK_END, onAdBreakEnd)
            ads?.addEventListener(AdsEventTypes.AD_SKIP, onAdSkip)
            ads?.addEventListener(AdsEventTypes.AD_ERROR, onAdError)

            ads?.removeEventListener(GoogleImaAdEventType.STARTED, onImaAdStarted)
            ads?.removeEventListener(GoogleImaAdEventType.COMPLETED, onImaAdCompleted)
            ads?.removeEventListener(GoogleImaAdEventType.SKIPPED, onImaAdSkip)
            ads?.removeEventListener(GoogleImaAdEventType.AD_BUFFERING, onImaAdBuffering)
            ads?.removeEventListener(GoogleImaAdEventType.AD_ERROR, onImaAdError)
            ads?.removeEventListener(
                GoogleImaAdEventType.CONTENT_RESUME_REQUESTED,
                onImaContentResume
            )
        }
    }

    private fun handleAdBreakBegin(adBreak: AdBreak?, isLinearAdBreak: Boolean) {
        // Make sure the session is started
        convivaHandler.maybeReportPlaybackRequested()

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
                calculateAdType(adBreak),
                calculateCurrentAdBreakInfo(adBreak, adBreakCounter)
            )
        } else {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "handleAdBreakBegin - No valid adBreak")
            }
        }
    }

    private fun handleAdBegin(ad: Ad?) {
        if (currentAdBreak == null && ad != null) {
            handleAdBreakBegin(ad.adBreak, isAdLinear(ad))
        }
        if (ad != null && isAdLinear(ad)) {
            currentAd = ad
            // Every session ad or content has its session ID. In order to “attach” an ad to its respective content session,
            // there are two tags that are critical:
            // - `c3.csid`: the content’s sessionID;
            // - `contentAssetName`: the content's assetName.
            val contentAssetName = convivaHandler.contentAssetName
            val adTechnology = calculateAdTypeAsString(ad)
            var adMetadata = collectAdMetadata(ad) + mapOf(
                "c3.csid" to convivaVideoAnalytics.sessionId.toString(),
                "contentAssetName" to contentAssetName,
                "c3.ad.technology" to adTechnology,
                ConvivaSdkConstants.IS_LIVE to false,
            )
            if (ad is GoogleImaAd) {
                // Update with Google IMA specific information
                adMetadata = updateAdMetadataForGoogleIma(ad, adMetadata)
            }

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
            if (ad is GoogleImaAd) {
                convivaAdAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.BITRATE,
                    player.videoWidth,
                    ad.imaAd.vastMediaBitrate
                )
            } else {
                convivaAdAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.BITRATE,
                    player.videoWidth
                )
            }

            // Report playing state in case of SSAI, as the player will not send an additional
            // `playing` event.
            if (calculateAdType(ad) == ConvivaSdkConstants.AdType.SERVER_SIDE) {
                convivaAdAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.PLAYING
                )
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "handleAdEnd - No valid ad")
            }
        }
    }

    private fun handleAdEnd(ad: Ad?) {
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

    private fun handleAdSkip() {
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

    private fun handleAdError() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "reportAdFailed")
        }
        convivaAdAnalytics.reportAdFailed("Ad Request Failed")
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
