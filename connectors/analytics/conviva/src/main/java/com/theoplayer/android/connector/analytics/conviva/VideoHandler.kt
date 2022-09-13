package com.theoplayer.android.connector.analytics.conviva

import com.conviva.sdk.ConvivaExperienceAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.event.player.*
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription

class VideoHandler : ConvivaExperienceAnalytics.ICallback {

    private val player: Player
    private val videoAnalytics: ConvivaVideoAnalytics

    private var currentSource: SourceDescription? = null
    private var isPlaybackRequested = false
    private var currentTime: Double = 0.0
    private var buffered: Double = 0.0
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    constructor(player: Player, videoAnalytics: ConvivaVideoAnalytics) {
        this.player = player
        this.videoAnalytics = videoAnalytics
        this.videoAnalytics.setCallback(this)
        attachListeners()
    }

    private fun attachListeners() {
        player.addEventListener(PlayerEventTypes.TIMEUPDATE, this::handleTimeUpdateEvent)
        player.addEventListener(PlayerEventTypes.PLAY, this::handlePlayEvent)
        player.addEventListener(PlayerEventTypes.PLAYING, this::handlePlayingEvent)
        player.addEventListener(PlayerEventTypes.PAUSE, this::handlePauseEvent)
        player.addEventListener(PlayerEventTypes.WAITING, this::handleWaitingEvent)
        player.addEventListener(PlayerEventTypes.SEEKING, this::handleSeekingEvent)
        player.addEventListener(PlayerEventTypes.SEEKED, this::handleSeekedEvent)
        player.addEventListener(PlayerEventTypes.ERROR, this::handleErrorEvent)
        player.addEventListener(PlayerEventTypes.SEGMENTNOTFOUND, this::handleSegmentNotFoundEvent)
        player.addEventListener(PlayerEventTypes.SOURCECHANGE, this::handleSourceChangeEvent)
        player.addEventListener(PlayerEventTypes.ENDED, this::handleEndedEvent)
        player.addEventListener(PlayerEventTypes.DURATIONCHANGE, this::handleDurationChangeEvent)
        player.addEventListener(PlayerEventTypes.RESIZE, this::handleResizeEvent)
    }

    private fun handleTimeUpdateEvent(timeUpdateEvent: TimeUpdateEvent) {
        currentTime = timeUpdateEvent.currentTime
        player.requestBuffered { timeRanges ->
            if (timeRanges.length() > 0) {
                buffered = timeRanges.getEnd(timeRanges.length() - 1)
            }
        }
        player.requestVideoWidth { videoWidth = it }
        player.requestVideoHeight { videoHeight = it }
    }

    private fun handlePlayEvent(playEvent: PlayEvent) {
        if (isPlaybackRequested) {
            return
        }
        isPlaybackRequested = true
        videoAnalytics.reportPlaybackRequested()
    }

    private fun handlePlayingEvent(playingEvent: PlayingEvent) {
        videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
    }

    private fun handlePauseEvent(pauseEvent: PauseEvent) {
        videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PAUSED)
    }

    private fun handleWaitingEvent(waitingEvent: WaitingEvent) {
        videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING)
    }

    private fun handleSeekingEvent(seekingEvent: SeekingEvent) {
        videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_STARTED)
    }

    private fun handleSeekedEvent(seekedEvent: SeekedEvent) {
        videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_ENDED)
    }

    private fun handleErrorEvent(errorEvent: ErrorEvent) {
        videoAnalytics.reportPlaybackFailed(errorEvent.errorObject.message)
    }

    private fun handleSegmentNotFoundEvent(segmentNotFoundEvent: SegmentNotFoundEvent) {
        videoAnalytics.reportPlaybackError(segmentNotFoundEvent.error, ConvivaSdkConstants.ErrorSeverity.FATAL)
    }

    private fun handleSourceChangeEvent(sourceChangeEvent: SourceChangeEvent) {
        if (currentSource == player.source) {
            return
        }
        maybeReportPlaybackEnded()
        isPlaybackRequested = false
        currentSource = player.source

        val contentInfo = HashMap<String, Any>()
        contentInfo[ConvivaSdkConstants.PLAYER_NAME] = "THEOplayer"
        contentInfo[ConvivaSdkConstants.STREAM_URL] = player.src!!
        videoAnalytics.setContentInfo(contentInfo)
    }

    private fun handleEndedEvent(endedEvent: EndedEvent) {
        videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.STOPPED)
        maybeReportPlaybackEnded()
    }

    private fun handleDurationChangeEvent(durationChangeEvent: DurationChangeEvent) {
        val contentInfo = HashMap<String, Any>()
        if (player.duration.isFinite()) {
            contentInfo[ConvivaSdkConstants.IS_LIVE] = ConvivaSdkConstants.StreamType.VOD
            contentInfo[ConvivaSdkConstants.DURATION] = player.duration
        } else {
            contentInfo[ConvivaSdkConstants.IS_LIVE] = ConvivaSdkConstants.StreamType.LIVE
        }
        videoAnalytics.setContentInfo(contentInfo)
    }

    private fun handleResizeEvent(resizeEvent: ResizeEvent) {
        videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, resizeEvent.width, resizeEvent.height)
    }

    private fun maybeReportPlaybackEnded() {
        if (isPlaybackRequested) {
            videoAnalytics.reportPlaybackEnded()
            isPlaybackRequested = false
        }
    }

    override fun update() {
        videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAY_HEAD_TIME, currentTime)
        videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.BUFFER_LENGTH, buffered)
        videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, videoWidth, videoHeight)
        player.videoTracks
            .first { it.isEnabled }
            .activeQuality?.let { videoQuality ->
                videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, videoQuality.bandwidth / 1000)
                videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE, videoQuality.frameRate)
            }
    }

    override fun update(str: String?) {

    }

    private fun removeListeners() {
        player.removeEventListener(PlayerEventTypes.TIMEUPDATE, this::handleTimeUpdateEvent)
        player.removeEventListener(PlayerEventTypes.PLAY, this::handlePlayEvent)
        player.removeEventListener(PlayerEventTypes.PLAYING, this::handlePlayingEvent)
        player.removeEventListener(PlayerEventTypes.PAUSE, this::handlePauseEvent)
        player.removeEventListener(PlayerEventTypes.WAITING, this::handleWaitingEvent)
        player.removeEventListener(PlayerEventTypes.SEEKING, this::handleSeekingEvent)
        player.removeEventListener(PlayerEventTypes.SEEKED, this::handleSeekedEvent)
        player.removeEventListener(PlayerEventTypes.ERROR, this::handleErrorEvent)
        player.removeEventListener(PlayerEventTypes.SEGMENTNOTFOUND, this::handleSegmentNotFoundEvent)
        player.removeEventListener(PlayerEventTypes.SOURCECHANGE, this::handleSourceChangeEvent)
        player.removeEventListener(PlayerEventTypes.ENDED, this::handleEndedEvent)
        player.removeEventListener(PlayerEventTypes.DURATIONCHANGE, this::handleDurationChangeEvent)
        player.removeEventListener(PlayerEventTypes.RESIZE, this::handleResizeEvent)
    }

    fun reset() {
        currentTime = 0.0
        buffered = 0.0
        videoWidth = 0
        videoHeight = 0
    }

    fun destroy() {
        // releasing of videoAnalytics is done in ConvivaConnector
        maybeReportPlaybackEnded()
        removeListeners()
        currentSource = null
        isPlaybackRequested = false
    }

}