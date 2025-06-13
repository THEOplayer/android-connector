package com.theoplayer.android.connector.analytics.adscript

import android.app.Activity
import android.util.Log
import com.nad.adscriptapiclient.AdScriptCollector
import com.nad.adscriptapiclient.AdScriptDataObject
import com.nad.adscriptapiclient.AdScriptEventEnum
import com.nad.adscriptapiclient.AdScriptI12n
import com.nad.adscriptapiclient.AdScriptPlayerState
import com.nad.adscriptapiclient.AdScriptRunnable
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.LinearAd
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.ads.AdBeginEvent
import com.theoplayer.android.api.event.ads.AdBreakBeginEvent
import com.theoplayer.android.api.event.ads.AdBreakEndEvent
import com.theoplayer.android.api.event.ads.AdEndEvent
import com.theoplayer.android.api.event.ads.AdFirstQuartileEvent
import com.theoplayer.android.api.event.ads.AdIntegrationKind
import com.theoplayer.android.api.event.ads.AdMidpointEvent
import com.theoplayer.android.api.event.ads.AdThirdQuartileEvent
import com.theoplayer.android.api.event.ads.AdsEventTypes
import com.theoplayer.android.api.event.player.DurationChangeEvent
import com.theoplayer.android.api.event.player.EndedEvent
import com.theoplayer.android.api.event.player.ErrorEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.event.player.PresentationModeChange
import com.theoplayer.android.api.event.player.RateChangeEvent
import com.theoplayer.android.api.event.player.SourceChangeEvent
import com.theoplayer.android.api.event.player.TimeUpdateEvent
import com.theoplayer.android.api.event.player.VolumeChangeEvent
import com.theoplayer.android.api.player.PresentationMode

private const val TAG = "AdscriptConnector"

data class LogPoint(
     val name: AdScriptEventEnum, val cue: Double
)

class AdscriptAdapter(
    activity: Activity,
    private val configuration: AdscriptConfiguration,
    private val playerView: THEOplayerView,
    private var contentMetadata: AdScriptDataObject?,
    private val adProcessor: AdProcessor?
) {
    private var adMetadata: AdScriptDataObject? = null
    private var waitingForFirstSecondOfAd = false
    private var waitingForFirstSecondOfSsaiAdSince: Double? = null
    private var contentLogPoints = ArrayDeque<LogPoint>()
    private val adScriptCollector = AdScriptCollector(configuration.implementationId)
    private val onPlay: EventListener<PlayEvent>
    private val onFirstPlaying: EventListener<PlayingEvent>
    private val onError: EventListener<ErrorEvent>
    private val onSourceChange: EventListener<SourceChangeEvent>
    private val onEnded: EventListener<EndedEvent>
    private val onDurationChange: EventListener<DurationChangeEvent>
    private val onTimeUpdate: EventListener<TimeUpdateEvent>
    private val onVolumeChange: EventListener<VolumeChangeEvent>
    private val onRateChange: EventListener<RateChangeEvent>
    private val onPresentationModeChange: EventListener<PresentationModeChange>
    private val onAdBreakStarted: EventListener<AdBreakBeginEvent>
    private val onAdStarted: EventListener<AdBeginEvent>
    private val onAdFirstQuartile: EventListener<AdFirstQuartileEvent>
    private val onAdMidpoint: EventListener<AdMidpointEvent>
    private val onAdThirdQuartile: EventListener<AdThirdQuartileEvent>
    private val onAdCompleted: EventListener<AdEndEvent>
    private val onAdBreakEnded: EventListener<AdBreakEndEvent>

    init {
        Thread(AdScriptRunnable(adScriptCollector, activity)).start()

        onPlay = EventListener { event -> handlePlay(event) }
        onFirstPlaying = EventListener { event -> handleFirstPlaying(event) }
        onError = EventListener { event -> handleError(event) }
        onSourceChange = EventListener { event -> handleSourceChange(event) }
        onEnded = EventListener { event -> handleEnded(event) }
        onDurationChange = EventListener { event -> handleDurationChange(event) }
        onTimeUpdate = EventListener { event -> handleTimeUpdate(event) }
        onVolumeChange = EventListener { event -> handleVolumeChange(event) }
        onRateChange = EventListener { event -> handleRateChange(event) }
        onPresentationModeChange = EventListener { event -> handlePresentationModeChange(event) }
        onAdBreakStarted = EventListener { event -> handleAdBreakStarted(event) }
        onAdStarted = EventListener { event -> handleAdStarted(event) }
        onAdFirstQuartile = EventListener { event -> handleAdFirstQuartile(event) }
        onAdMidpoint = EventListener { event -> handleAdMidpoint(event) }
        onAdThirdQuartile = EventListener { event -> handleAdThirdQuartile(event) }
        onAdCompleted = EventListener { event -> handleAdCompleted(event) }
        onAdBreakEnded = EventListener { event -> handleAdBreakEnded(event) }

        adScriptCollector.playerState = AdScriptPlayerState()
        reportPlayerState()
        addEventListeners()
    }

    fun start(){
        adScriptCollector.sessionStart()
    }

    fun update(metadata: AdScriptDataObject) {
        contentMetadata = metadata
    }

    fun updateUser(i12n: AdScriptI12n) {
        adScriptCollector.i12n = i12n
    }

    private fun reportPlayerState(){
        reportFullscreen(playerView.fullScreenManager.isFullScreen)
        reportDimensions(playerView.player.videoWidth, playerView.player.videoHeight)
        reportPlaybackSpeed(playerView.player.playbackRate)
        reportVolumeAndMuted(playerView.player.isMuted, playerView.player.volume)
        reportTriggeredByUser(playerView.player.isAutoplay)
        reportVisibility()
    }

    private fun reportFullscreen(isFullscreen: Boolean) {
        if (isFullscreen) {
            this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_fullscreen, 1)
        } else {
            this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_fullscreen, 0)
        }
    }

    private fun reportPlaybackSpeed(playbackRate: Double) {
        if (playbackRate == 1.0) {
            this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_normalSpeed, 1)
        } else {
            this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_normalSpeed, 0)
        }
    }

    private fun reportDimensions(width: Int, height: Int) {
        this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_height, width)
        this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_width, height)
    }

    private fun reportVolumeAndMuted(isMuted: Boolean, volume: Double) {
        if (isMuted || volume == 0.0) {
            if (configuration.debug) {
                Log.i(TAG,"Reporting muted (1) & volume (0)")
            }
            this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_muted, 1)
            this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_volume, 0)
        } else {
            val volumePercentage = (volume * 100).toInt()
            if (configuration.debug) {
                Log.i(TAG,"Reporting muted (0) & volume ($volumePercentage)")
            }
            this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_muted, 0)
            this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_volume, volumePercentage)
        }
    }

    private fun reportTriggeredByUser(isAutoplay: Boolean) {
        if (isAutoplay) {
            this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_triggeredByUser, 1)
        } else {
            this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_triggeredByUser, 0)
        }
    }

    private fun reportVisibility() {
        this.adScriptCollector.playerState.set(AdScriptPlayerState.FIELD_visibility, 100)
    }

    private fun reportLogPoint(name: AdScriptEventEnum) {
        this.adScriptCollector.push(name, contentMetadata)
    }

    private fun addLogPoints(duration: Double) {
        if (duration.isFinite()) {
            contentLogPoints.addLast(LogPoint(AdScriptEventEnum.PROGRESS1, 1.0))
            contentLogPoints.addLast(LogPoint(AdScriptEventEnum.FIRSTQUARTILE, duration * 0.25))
            contentLogPoints.addLast(LogPoint(AdScriptEventEnum.MIDPOINT, duration * 0.5))
            contentLogPoints.addLast(LogPoint(AdScriptEventEnum.THIRDQUARTILE, duration * 0.75))
        } else {
            contentLogPoints.addLast(LogPoint(AdScriptEventEnum.PROGRESS1, 1.0))
        }
    }

    private fun maybeReportProgress(currentTime: Double) {
        if (playerView.player.ads.isPlaying) {
            maybeReportAdProgress(currentTime)
            return
        }
        val nextLogPoint = contentLogPoints.firstOrNull()
        if (nextLogPoint != null && currentTime >= nextLogPoint.cue ) {
            reportLogPoint(nextLogPoint.name)
            contentLogPoints.removeFirst()
        }
    }

    private fun maybeReportAdProgress(currentTime: Double) {
        if (!waitingForFirstSecondOfAd) return
        val currentAd = playerView.player.ads.currentAds.firstOrNull()
        when (currentAd?.integration) {
            AdIntegrationKind.GOOGLE_IMA -> {
                if (currentTime >= 1) {
                    adScriptCollector.push(AdScriptEventEnum.PROGRESS1,adMetadata)
                    waitingForFirstSecondOfAd = false
                }
            }
            AdIntegrationKind.GOOGLE_DAI -> {
                val waitingSince = waitingForFirstSecondOfSsaiAdSince
                if (waitingSince != null && currentTime >= waitingSince + 1.0){
                    adScriptCollector.push(AdScriptEventEnum.PROGRESS1,adMetadata)
                    waitingForFirstSecondOfAd = false
                    waitingForFirstSecondOfSsaiAdSince = null
                }
            }

            AdIntegrationKind.THEO_ADS -> TODO()
            AdIntegrationKind.MEDIATAILOR -> TODO()
            AdIntegrationKind.CUSTOM -> TODO()
            null -> TODO()
        }
    }

    private fun getAdType(timeOffset: Int?): String {
        return when (timeOffset) {
            0 -> {
                AdScriptDataObject.OBJ_TYPE_preroll
            }
            -1, playerView.player.duration.toInt() -> {
                AdScriptDataObject.OBJ_TYPE_postroll
            }
            else -> {
                AdScriptDataObject.OBJ_TYPE_midroll
            }
        }
    }

    private fun buildAdMetadata(ad: Ad?) {
        if (adProcessor != null && ad != null) {
            adMetadata = adProcessor.apply(ad)
        } else {
            val currentAdMetadata = AdScriptDataObject()
            currentAdMetadata.set(AdScriptDataObject.FIELD_assetId, ad?.id)
            currentAdMetadata.set(AdScriptDataObject.FIELD_type, getAdType(ad?.adBreak?.timeOffset))
            if (ad is LinearAd) {
                currentAdMetadata.set(AdScriptDataObject.FIELD_length, ad.duration)
            }
            currentAdMetadata.set(AdScriptDataObject.FIELD_title, ad?.id)
//            currentAdMetadata.set(AdScriptDataObject.FIELD_asmea, "TODO")
            currentAdMetadata.set(AdScriptDataObject.FIELD_attribute, AdScriptDataObject.ATTRIBUTE_Commercial)
            adMetadata = currentAdMetadata
        }
    }

    private fun addEventListeners(){
        playerView.player.addEventListener(PlayerEventTypes.PLAY, onPlay)
        playerView.player.addEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
        playerView.player.addEventListener(PlayerEventTypes.ERROR, onError)
        playerView.player.addEventListener(PlayerEventTypes.SOURCECHANGE, onSourceChange)
        playerView.player.addEventListener(PlayerEventTypes.ENDED, onEnded)
        playerView.player.addEventListener(PlayerEventTypes.DURATIONCHANGE, onDurationChange)
        playerView.player.addEventListener(PlayerEventTypes.TIMEUPDATE, onTimeUpdate)
        playerView.player.addEventListener(PlayerEventTypes.VOLUMECHANGE, onVolumeChange)
        playerView.player.addEventListener(PlayerEventTypes.RATECHANGE, onRateChange)
        playerView.player.addEventListener(PlayerEventTypes.PRESENTATIONMODECHANGE, onPresentationModeChange)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_BREAK_BEGIN, onAdBreakStarted)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_BEGIN, onAdStarted)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_FIRST_QUARTILE, onAdFirstQuartile)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_MIDPOINT, onAdMidpoint)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_THIRD_QUARTILE, onAdThirdQuartile)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_END, onAdCompleted)
    }

//    private fun removeEventListeners(){
//        playerView.player.removeEventListener(PlayerEventTypes.PLAY, onPlay)
//        playerView.player.removeEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
//        playerView.player.removeEventListener(PlayerEventTypes.ERROR, onError)
//        playerView.player.removeEventListener(PlayerEventTypes.SOURCECHANGE, onSourceChange)
//        playerView.player.removeEventListener(PlayerEventTypes.ENDED, onEnded)
//        playerView.player.removeEventListener(PlayerEventTypes.DURATIONCHANGE, onDurationChange)
//        playerView.player.removeEventListener(PlayerEventTypes.TIMEUPDATE, onTimeUpdate)
//        playerView.player.removeEventListener(PlayerEventTypes.VOLUMECHANGE, onVolumeChange)
//        playerView.player.removeEventListener(PlayerEventTypes.RATECHANGE, onRateChange)
//        playerView.player.removeEventListener(PlayerEventTypes.PRESENTATIONMODECHANGE, onPresentationModeChange)
//        playerView.player.ads.removeEventListener(AdsEventTypes.AD_BREAK_BEGIN, onAdBreakStarted)
//        playerView.player.ads.removeEventListener(AdsEventTypes.AD_BEGIN, onAdStarted)
//        playerView.player.ads.removeEventListener(AdsEventTypes.AD_FIRST_QUARTILE, onAdFirstQuartile)
//        playerView.player.ads.removeEventListener(AdsEventTypes.AD_MIDPOINT, onAdMidpoint)
//        playerView.player.ads.removeEventListener(AdsEventTypes.AD_THIRD_QUARTILE, onAdThirdQuartile)
//        playerView.player.ads.removeEventListener(AdsEventTypes.AD_END, onAdCompleted)
//    }

//    window.addEventListener('resize', this.reportPlayerState)
//    window.addEventListener('blur', this.reportPlayerState)
//    window.addEventListener('focus', this.reportPlayerState)
//    document.addEventListener('scroll', this.reportPlayerState)
//    document.addEventListener('visibilitychange', this.reportPlayerState)


    private fun handlePlay(event: PlayEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : currentTime = ${event.currentTime}")
        }
    }

    private fun handleFirstPlaying(event: PlayingEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : currentTime = ${event.currentTime}")
        }
        if (playerView.player.ads.isPlaying) {
            adScriptCollector.push(AdScriptEventEnum.START, adMetadata)
        } else {
            adScriptCollector.push(AdScriptEventEnum.START, contentMetadata)
        }
        playerView.player.removeEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
    }

    private fun handleError(event: ErrorEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : code = ${event.errorObject.code} ; message = ${event.errorObject.message}")
        }
    }

    private fun handleSourceChange(event: SourceChangeEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : source = ${event.source.toString()}")
        }
        playerView.player.addEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
    }

    private fun handleEnded(event: EndedEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : currentTime = ${event.currentTime}")
        }
        adScriptCollector.push(AdScriptEventEnum.COMPLETE, contentMetadata)
    }

    private fun handleDurationChange(event: DurationChangeEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : duration = ${event.duration}")
        }
        addLogPoints(event.duration)
    }

    private fun handleTimeUpdate(event: TimeUpdateEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : currentTime = ${event.currentTime}")
        }
        maybeReportProgress(event.currentTime)
    }

    private fun handleVolumeChange(event: VolumeChangeEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : currentTime = ${event.currentTime}")
        }
        reportVolumeAndMuted(playerView.player.isMuted, event.volume)
    }

    private fun handleRateChange(event: RateChangeEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : currentTime = ${event.currentTime}")
        }
        reportPlaybackSpeed(event.playbackRate)
    }

    private fun handlePresentationModeChange(event: PresentationModeChange) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : presentationMode = ${event.presentationMode}")
        }
        reportFullscreen(event.presentationMode === PresentationMode.FULLSCREEN)
    }

    private fun handleAdBreakStarted(event: AdBreakBeginEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : offset = ${event.adBreak.timeOffset}")
        }
    }

    private fun handleAdStarted(event: AdBeginEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : id = ${event.ad?.id}")
        }
        buildAdMetadata(event.ad)
        waitingForFirstSecondOfAd = true
        if (event.ad?.integration == AdIntegrationKind.GOOGLE_DAI) {
            waitingForFirstSecondOfSsaiAdSince = playerView.player.currentTime
        }
        adScriptCollector.push(AdScriptEventEnum.START, adMetadata)
    }

    private fun handleAdFirstQuartile(event: AdFirstQuartileEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : id = ${event.ad?.id}")
        }
        adScriptCollector.push(AdScriptEventEnum.FIRSTQUARTILE, adMetadata)

    }

    private fun handleAdMidpoint(event: AdMidpointEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : id = ${event.ad?.id}")
        }
        adScriptCollector.push(AdScriptEventEnum.MIDPOINT, adMetadata)
    }

    private fun handleAdThirdQuartile(event: AdThirdQuartileEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : id = ${event.ad?.id}")
        }
        adScriptCollector.push(AdScriptEventEnum.THIRDQUARTILE, adMetadata)
    }

    private fun handleAdCompleted(event: AdEndEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : id = ${event.ad?.id}")
        }
        adScriptCollector.push(AdScriptEventEnum.COMPLETE, adMetadata)

    }

    private fun handleAdBreakEnded(event: AdBreakEndEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type} : timeOffset = ${event.adBreak.timeOffset}")
        }
        if (event.adBreak.timeOffset == 0) {
            playerView.player.addEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
        }
    }
}