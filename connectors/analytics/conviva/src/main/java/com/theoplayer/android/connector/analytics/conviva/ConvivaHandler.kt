package com.theoplayer.android.connector.analytics.conviva

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaExperienceAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.event.EventDispatcher
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.ads.AdEvent
import com.theoplayer.android.api.event.player.*
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.PlaybackPipeline
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.connector.analytics.conviva.ads.AdReporter
import com.theoplayer.android.connector.analytics.conviva.utils.ErrorReportBuilder
import com.theoplayer.android.connector.analytics.conviva.utils.calculateBufferLength
import com.theoplayer.android.connector.analytics.conviva.utils.calculateConvivaOptions
import com.theoplayer.android.connector.analytics.conviva.utils.collectContentMetadata
import com.theoplayer.android.connector.analytics.conviva.utils.collectPlayerInfo
import java.lang.Double.isFinite

private const val TAG = "ConvivaHandler"

interface ConvivaHandlerBase {
    val contentAssetName: String

    fun maybeReportPlaybackRequested()
}

/**
 * ConvivaHandler
 *
 * https://pulse.conviva.com/learning-center/content/sensor_developer_center/sensor_integration/android/android_stream_sensor.htm
 */
@Suppress("SpellCheckingInspection")
class ConvivaHandler(
    appContext: Context,
    private val player: Player,
    private val convivaMetadata: ConvivaMetadata,
    convivaConfig: ConvivaConfiguration,
    adEventsExtension: EventDispatcher<AdEvent<*>>?
) : ConvivaExperienceAnalytics.ICallback, ConvivaHandlerBase {
    private lateinit var lifecycleObserver: LifecycleObserver
    private val mainHandler = Handler(Looper.getMainLooper())
    private var customMetadata: ConvivaMetadata = mapOf()

    private var convivaVideoAnalytics: ConvivaVideoAnalytics
    private var convivaAdAnalytics: ConvivaAdAnalytics

    private var adReporter: AdReporter? = null

    private var currentSource: SourceDescription? = null
    private var playbackRequested: Boolean = false

    private val onPlay: EventListener<PlayEvent>
    private val onPlaying: EventListener<PlayingEvent>
    private val onPause: EventListener<PauseEvent>
    private val onWaiting: EventListener<WaitingEvent>
    private val onSeeking: EventListener<SeekingEvent>
    private val onSeeked: EventListener<SeekedEvent>
    private val onError: EventListener<ErrorEvent>
    private val onSegmentNotFound: EventListener<SegmentNotFoundEvent>
    private val onSourceChange: EventListener<SourceChangeEvent>
    private val onEnded: EventListener<EndedEvent>
    private val onDurationChange: EventListener<DurationChangeEvent>
    private val errorReportBuilder = ErrorReportBuilder()

    init {
        ConvivaAnalytics.init(
            appContext,
            convivaConfig.customerKey,
            calculateConvivaOptions(convivaConfig)
        )

        // This object will be used throughout the entire application lifecycle to report video related events.
        convivaVideoAnalytics = ConvivaAnalytics.buildVideoAnalytics(appContext).apply {
            setPlayerInfo(collectPlayerInfo())
            setCallback(this@ConvivaHandler)
        }

        // This object will be used throughout the entire application lifecycle to report ad related events.
        convivaAdAnalytics = ConvivaAnalytics.buildAdAnalytics(appContext, convivaVideoAnalytics)

        adReporter = AdReporter(
            player,
            convivaVideoAnalytics,
            convivaAdAnalytics,
            this,
            adEventsExtension,
            )

        onPlay = EventListener<PlayEvent> {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onPlay")
            }
            maybeReportPlaybackRequested()
        }

        onPlaying = EventListener<PlayingEvent> {
            reportPlaying()
        }

        onPause = EventListener<PauseEvent> {
            reportPause()
        }

        onWaiting = EventListener<WaitingEvent> {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onWaiting")
            }
            convivaVideoAnalytics.reportPlaybackMetric(
                ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                ConvivaSdkConstants.PlayerState.BUFFERING
            )
        }

        onSeeking = EventListener<SeekingEvent> {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onSeeking")
            }
            convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_STARTED)
        }

        onSeeked = EventListener<SeekedEvent> {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onSeeked")
            }
            convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_ENDED)
        }

        onError = EventListener<ErrorEvent> { event ->
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onError: ${event.errorObject.message}")
            }

            val error = event.errorObject
            // Report error details in a separate event, which should be passed a flat <String, String> map.
            convivaVideoAnalytics.reportPlaybackEvent("ErrorDetailsEvent", errorReportBuilder.apply {
                withPlayerBuffer(player)
                withErrorDetails(event.errorObject)
            }.build())

            // Report error and cleanup immediately.
            // The contentInfo provides metadata for the failed video.
            reportPlaybackFailed(error.message ?: "Fatal error occurred")
        }

        onSegmentNotFound = EventListener<SegmentNotFoundEvent> {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onSegmentNotFound")
            }
            // Report the error but keep the session open.
            // (e.g., in case of player internal re-try or fallback logic):
            convivaVideoAnalytics.reportPlaybackError(
                "A Video Playback Failure has occurred: Segment not found",
                ConvivaSdkConstants.ErrorSeverity.WARNING
            )
        }

        onSourceChange = EventListener<SourceChangeEvent> { event ->
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onSourceChange")
            }
            maybeReportPlaybackEnded()
            currentSource = player.source
            customMetadata = mapOf("playbackPipeline" to
                when(event.playbackPipeline) {
                    PlaybackPipeline.LEGACY -> "legacy"
                    PlaybackPipeline.MEDIA3 -> "media3"
                    else -> "NA"
                }
            )
        }

        onEnded = EventListener<EndedEvent> {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onEnded")
            }
            convivaVideoAnalytics.reportPlaybackMetric(
                ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                ConvivaSdkConstants.PlayerState.STOPPED
            )
            maybeReportPlaybackEnded()
        }

        onDurationChange = EventListener<DurationChangeEvent> {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onDurationChange")
            }
            val contentInfo = HashMap<String, Any>()
            if (player.duration.isFinite()) {
                contentInfo[ConvivaSdkConstants.IS_LIVE] = ConvivaSdkConstants.StreamType.VOD

                // Report duration; Int (seconds)
                contentInfo[ConvivaSdkConstants.DURATION] = player.duration.toInt()
            } else {
                contentInfo[ConvivaSdkConstants.IS_LIVE] = ConvivaSdkConstants.StreamType.LIVE
            }
            convivaVideoAnalytics.setContentInfo(contentInfo)
        }

        // Listen for player events
        addEventListeners()
    }

    fun stopAndStartNewSession(metadata: ConvivaMetadata) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "stopAndStartNewSession")
        }

        // End current session if one had already started
        maybeReportPlaybackEnded()

        // Start new session
        maybeReportPlaybackRequested()

        // Pass new metadata
        setContentInfo(metadata)

        // Notify current playback state
        if (player.isPaused) {
            reportPause()
        } else {
            reportPlaying()
        }
    }

    fun setContentInfo(metadata: ConvivaMetadata) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setContentInfo")
        }
        customMetadata = customMetadata + metadata
        convivaVideoAnalytics.setContentInfo(customMetadata)
    }

    fun setAdInfo(metadata: ConvivaMetadata) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setAdInfo")
        }
        convivaAdAnalytics.setAdInfo(metadata)
    }

    private fun reportPlaying() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPlaying")
        }
        convivaVideoAnalytics.reportPlaybackMetric(
            ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
            ConvivaSdkConstants.PlayerState.PLAYING
        )
    }

    private fun reportPause() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPause")
        }
        convivaVideoAnalytics.reportPlaybackMetric(
            ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
            ConvivaSdkConstants.PlayerState.PAUSED
        )
    }

    fun reportPlaybackFailed(errorMessage: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "reportPlaybackFailed: $errorMessage")
        }

        // Report error and cleanup immediately.
        // The contentInfo provides metadata for the failed video.
        if (player.duration.isNaN()) {
            convivaVideoAnalytics.reportPlaybackFailed(errorMessage, mapOf<String, Any>(ConvivaSdkConstants.DURATION to -1))
        } else {
            convivaVideoAnalytics.reportPlaybackFailed(errorMessage)
        }
    }

    fun reportPlaybackEvent(eventType: String, eventDetail: Map<String, Any>?) {
        this.convivaVideoAnalytics.reportPlaybackEvent(eventType, eventDetail)
    }

    private fun addEventListeners() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "addEventListeners")
        }
        player.addEventListener(PlayerEventTypes.PLAY, onPlay)
        player.addEventListener(PlayerEventTypes.PLAYING, onPlaying)
        player.addEventListener(PlayerEventTypes.PAUSE, onPause)
        player.addEventListener(PlayerEventTypes.WAITING, onWaiting)
        player.addEventListener(PlayerEventTypes.SEEKING, onSeeking)
        player.addEventListener(PlayerEventTypes.SEEKED, onSeeked)
        player.addEventListener(PlayerEventTypes.ERROR, onError)
        player.addEventListener(PlayerEventTypes.SEGMENTNOTFOUND, onSegmentNotFound)
        player.addEventListener(PlayerEventTypes.SOURCECHANGE, onSourceChange)
        player.addEventListener(PlayerEventTypes.ENDED, onEnded)
        player.addEventListener(PlayerEventTypes.DURATIONCHANGE, onDurationChange)

        // Observe app switches between background and foreground
        lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "reportAppBackgrounded")
                    }
                    ConvivaAnalytics.reportAppBackgrounded()
                }
                Lifecycle.Event.ON_START -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "reportAppForegrounded")
                    }
                    ConvivaAnalytics.reportAppForegrounded()
                }
                Lifecycle.Event.ON_DESTROY -> destroy()
                else -> {/*ignore*/
                }
            }
        }
        mainHandler.post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        }

        player.network.addHTTPInterceptor(errorReportBuilder)
    }

    private fun removeEventListeners() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "removeEventListeners")
        }
        player.removeEventListener(PlayerEventTypes.PLAY, onPlay)
        player.removeEventListener(PlayerEventTypes.PLAYING, onPlaying)
        player.removeEventListener(PlayerEventTypes.PAUSE, onPause)
        player.removeEventListener(PlayerEventTypes.WAITING, onWaiting)
        player.removeEventListener(PlayerEventTypes.SEEKING, onSeeking)
        player.removeEventListener(PlayerEventTypes.SEEKED, onSeeked)
        player.removeEventListener(PlayerEventTypes.ERROR, onError)
        player.removeEventListener(PlayerEventTypes.SEGMENTNOTFOUND, onSegmentNotFound)
        player.removeEventListener(PlayerEventTypes.SOURCECHANGE, onSourceChange)
        player.removeEventListener(PlayerEventTypes.ENDED, onEnded)
        player.removeEventListener(PlayerEventTypes.DURATIONCHANGE, onDurationChange)

        mainHandler.post {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        }

        player.network.removeHTTPInterceptor(errorReportBuilder)
    }

    // Update API will be called by Conviva SDK at regular intervals to compute playback
    // metrics. This update callback will be called at the frequency of 1sec
    override fun update() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "convivaCallback.update() ${(1e3 * player.currentTime).toLong()}")
        }

        // Report currentTime; Long (ms)
        convivaVideoAnalytics.reportPlaybackMetric(
            ConvivaSdkConstants.PLAYBACK.PLAY_HEAD_TIME,
            (1e3 * player.currentTime).toLong()
        )
        // Report current buffer length of the player: Long (ms)
        convivaVideoAnalytics.reportPlaybackMetric(
            ConvivaSdkConstants.PLAYBACK.BUFFER_LENGTH,
            calculateBufferLength(player)
        )
        // Report current media resolution
        convivaVideoAnalytics.reportPlaybackMetric(
            ConvivaSdkConstants.PLAYBACK.RESOLUTION,
            player.videoWidth,
            player.videoHeight
        )
        player.videoTracks
            .firstOrNull { it.isEnabled }
            ?.activeQuality?.let { videoQuality ->
                // Report current bitrate value; Int (kbps)
                convivaVideoAnalytics.reportPlaybackMetric(
                    ConvivaSdkConstants.PLAYBACK.BITRATE,
                    (videoQuality.bandwidth / 1000).toInt()
                )
                // Report current framerate; Int
                convivaVideoAnalytics.reportPlaybackMetric(
                    ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE,
                    videoQuality.frameRate.toInt()
                )
            }
    }

    override fun update(str: String?) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "convivaCallback.update(str)")
        }
    }

    /**
     * reportPlaybackRequested() if
     * - User clicks play button
     * - Video starts in autoplay mode
     * - User replays video again
     * - A new video starts in playlist
     */
    override fun maybeReportPlaybackRequested() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "maybeReportPlaybackRequested: $playbackRequested")
        }
        if (!playbackRequested) {
            playbackRequested = true

            // In most cases that it's required to set content metadata before the player reports
            // "play" for the first time, to accurately attribute metadata to the video asset.
            reportMetadata()

            convivaVideoAnalytics.reportPlaybackRequested(
                collectContentMetadata(player, convivaMetadata)
            )
        }
    }

    /**
     * reportPlaybackEnded() if
     * - User stops the video
     * - User starts another video
     * - Video ends
     * - Video item ends in playlist
     */
    private fun maybeReportPlaybackEnded() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "maybeReportPlaybackEnded: $playbackRequested")
        }
        if (playbackRequested) {
            adReporter?.reset()
            convivaVideoAnalytics.reportPlaybackEnded()
            playbackRequested = false
        }
    }

    override val contentAssetName: String
        get() {
            return if (customMetadata.containsKey(ConvivaSdkConstants.ASSET_NAME)) {
                customMetadata[ConvivaSdkConstants.ASSET_NAME] as String
            } else if (player.source?.metadata?.containsKey("title") == true) {
                player.source?.metadata?.get("title") as String
            } else {
                "NA"
            }
        }

    private fun reportMetadata() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "reportMetadata")
        }
        val src = player.src ?: ""
        val streamType = if (isFinite(player.duration)) {
            ConvivaSdkConstants.StreamType.VOD
        } else {
            ConvivaSdkConstants.StreamType.LIVE
        }
        val playerName = customMetadata[ConvivaSdkConstants.PLAYER_NAME] ?: "THEOplayer"
        setContentInfo(
            mapOf(
                ConvivaSdkConstants.STREAM_URL to src,
                ConvivaSdkConstants.IS_LIVE to streamType,
                ConvivaSdkConstants.ASSET_NAME to contentAssetName,
                ConvivaSdkConstants.PLAYER_NAME to playerName
            )
        )
    }

    /**
     * On application exit, or when the Conviva object is destroyed.
     */
    fun destroy() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "destroy")
        }
        maybeReportPlaybackEnded()
        removeEventListeners()

        adReporter?.destroy()
        customMetadata = mapOf()
        convivaAdAnalytics.release()
        convivaVideoAnalytics.release()
        ConvivaAnalytics.release()
    }
}
