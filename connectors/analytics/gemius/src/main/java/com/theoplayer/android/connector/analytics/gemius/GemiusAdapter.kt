package com.theoplayer.android.connector.analytics.gemius

import android.content.Context
import android.util.Log
import com.gemius.sdk.stream.EventAdData
import com.gemius.sdk.stream.EventProgramData
import com.theoplayer.android.api.THEOplayerView
import com.gemius.sdk.stream.Player
import com.gemius.sdk.stream.PlayerData
import com.gemius.sdk.stream.ProgramData
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.LinearAd
import com.theoplayer.android.api.ads.ima.GoogleImaAd
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.ads.AdBeginEvent
import com.theoplayer.android.api.event.ads.AdBreakBeginEvent
import com.theoplayer.android.api.event.ads.AdBreakEndEvent
import com.theoplayer.android.api.event.ads.AdEndEvent
import com.theoplayer.android.api.event.ads.AdSkipEvent
import com.theoplayer.android.api.event.ads.AdsEventTypes
import com.theoplayer.android.api.event.player.EndedEvent
import com.theoplayer.android.api.event.player.ErrorEvent
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.event.player.SeekingEvent
import com.theoplayer.android.api.event.player.SourceChangeEvent
import com.theoplayer.android.api.event.player.VolumeChangeEvent
import com.theoplayer.android.api.event.player.WaitingEvent
import com.theoplayer.android.api.event.track.mediatrack.audio.QualityChangedEvent
import com.theoplayer.android.api.event.track.mediatrack.video.list.AddTrackEvent
import com.theoplayer.android.api.event.track.mediatrack.video.list.RemoveTrackEvent
import com.theoplayer.android.api.event.track.mediatrack.video.list.VideoTrackListEventTypes
import com.theoplayer.android.api.event.track.tracklist.TrackListEvent

const val PLAYER_ID = "THEOplayer"
const val TAG = "GemiusConnector"

class GemiusAdapter(
    context: Context,
    private val configuration: GemiusConfiguration,
    private val playerView: THEOplayerView,
    private val adProcessor: AdProcessor?
) {
    private val gemiusPlayer: Player?

    private var programId: String? = null
    private var programData: ProgramData? = null

    private var partCount: Int = 1
    private var adCount: Int = 1
    private var currentAd: Ad? = null

    private val onSourceChange: EventListener<SourceChangeEvent>
    private val onFirstPlaying: EventListener<PlayingEvent>
//    private val onPlay: EventListener<PlayEvent>
    private val onPause: EventListener<PauseEvent>
    private val onWaiting: EventListener<WaitingEvent>
    private val onSeeking: EventListener<SeekingEvent>
    private val onError: EventListener<ErrorEvent>
    private val onEnded: EventListener<EndedEvent>
    private val onVolumeChange: EventListener<VolumeChangeEvent>

    private val onAddVideoTrack: EventListener<AddTrackEvent>
    private val onRemoveVideoTrack: EventListener<RemoveTrackEvent>
    private val onVideoQualityChanged: EventListener<QualityChangedEvent<*,*>>

    private val onAdBreakBeginListener: EventListener<AdBreakBeginEvent>
    private val onAdBeginListener: EventListener<AdBeginEvent>
    private val onAdEndListener: EventListener<AdEndEvent>
    private val onAdSkipListener: EventListener<AdSkipEvent>
    private val onAdBreakEndedListener: EventListener<AdBreakEndEvent>

    init {
        val playerData = PlayerData()
        playerData.resolution = "${playerView.width}x${playerView.height}"
        playerData.volume = computeVolume()
        gemiusPlayer = Player(PLAYER_ID, configuration.hitCollectorHost, configuration.gemiusId, playerData)
        gemiusPlayer.setContext(context)

        onSourceChange = EventListener { event -> handleSourceChange(event) }
        onFirstPlaying = EventListener { event -> handleFirstPlaying(event) }
//        onPlay = EventListener { event -> handlePlay(event) }
        onPause = EventListener { event -> handlePause(event) }
        onWaiting = EventListener { event -> handleWaiting(event) }
        onSeeking = EventListener { event -> handleSeeking(event) }
        onError = EventListener { event -> handleError(event) }
        onEnded = EventListener { event -> handleEnded(event) }
        onVolumeChange = EventListener { event -> handleVolumeChange(event) }
        onAddVideoTrack = EventListener { event -> handleAddVideoTrack(event) }
        onRemoveVideoTrack = EventListener { event -> handleRemoveVideoTrack(event) }
        onVideoQualityChanged = EventListener { event -> handleVideoQualityChanged(event) }
        onAdBreakBeginListener = EventListener { event -> handleAdBreakBegin(event) }
        onAdBeginListener = EventListener { event -> handleAdBegin(event) }
        onAdEndListener = EventListener { event -> handleAdEnd(event) }
        onAdSkipListener = EventListener { event -> handleAdSkip(event) }
        onAdBreakEndedListener = EventListener { event -> handleAdBreakEnded(event) }

        addEventListeners()
    }

    fun update(programId: String, programData: ProgramData) {
        this.programId = programId
        this.programData = programData
    }

    private fun computeVolume(): Int {
        return if (playerView.player.isMuted) -1 else (playerView.player.volume * 100).toInt()
    }

    private fun addEventListeners() {
        playerView.player.addEventListener(PlayerEventTypes.SOURCECHANGE, onSourceChange)
        playerView.player.addEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
//        playerView.player.addEventListener(PlayerEventTypes.PLAY, onPlay)
        playerView.player.addEventListener(PlayerEventTypes.PAUSE, onPause)
        playerView.player.addEventListener(PlayerEventTypes.WAITING, onWaiting)
        playerView.player.addEventListener(PlayerEventTypes.SEEKING, onSeeking)
        playerView.player.addEventListener(PlayerEventTypes.ERROR, onError)
        playerView.player.addEventListener(PlayerEventTypes.ENDED, onEnded)
        playerView.player.addEventListener(PlayerEventTypes.VOLUMECHANGE, onVolumeChange)
        playerView.player.videoTracks.addEventListener(VideoTrackListEventTypes.ADDTRACK, onAddVideoTrack)
        playerView.player.videoTracks.addEventListener(VideoTrackListEventTypes.REMOVETRACK, onRemoveVideoTrack)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_BREAK_BEGIN, onAdBreakBeginListener)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_BEGIN, onAdBeginListener)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_END, onAdEndListener)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_SKIP, onAdSkipListener)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_BREAK_END, onAdBreakEndedListener)
    }

    private fun handleSourceChange(event: SourceChangeEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: source = ${event.source.toString()}")
        }
        partCount = 1
        currentAd = null
        val programId = this.programId ?: return
        val programData = this.programData ?: return
        gemiusPlayer?.newProgram(programId,programData)
        playerView.player.removeEventListener(PlayerEventTypes.PLAYING,onFirstPlaying)
        playerView.player.addEventListener(PlayerEventTypes.PLAYING,onFirstPlaying)
    }
    private fun handleFirstPlaying(event: PlayingEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: currentTime = ${event.currentTime}")
        }
        val computedVolume = computeVolume()
        val programId = programId ?: return
        currentAd?.let { ad ->
            val adId = ad.id
            val adBreak = ad.adBreak ?: return
            val offset = adBreak.timeOffset
            val adEventData = EventAdData()
            adEventData.volume = computedVolume
            adEventData.breakSize = adBreak.ads.size
            if (ad is LinearAd) adEventData.adDuration = ad.duration
            adEventData.adPosition = adCount
            gemiusPlayer?.adEvent(programId, adId, offset, Player.EventType.PLAY, adEventData)
        } ?: run {
            val player = playerView.player
            val currentQuality = player.videoTracks.first { track -> track.isEnabled }.activeQuality
            val programEventData = EventProgramData()
            programEventData.volume = computedVolume
            programEventData.programDuration = player.duration.toInt()
            programEventData.partID = partCount
            programEventData.autoPlay = player.isAutoplay
            if (currentQuality!= null) programEventData.quality = "${currentQuality.width}x${currentQuality.height}"
            gemiusPlayer?.programEvent(programId, player.currentTime.toInt(), Player.EventType.PLAY, programEventData)
        }
    }
    private fun handlePause(event: PauseEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleWaiting(event: WaitingEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleSeeking(event: SeekingEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleError(event: ErrorEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleEnded(event: EndedEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleVolumeChange(event: VolumeChangeEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleAddVideoTrack(event: AddTrackEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleRemoveVideoTrack(event: RemoveTrackEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleVideoQualityChanged(event: QualityChangedEvent<*,*>) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleAdBreakBegin(event: AdBreakBeginEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleAdBegin(event: AdBeginEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleAdEnd(event: AdEndEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleAdSkip(event: AdSkipEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }
    private fun handleAdBreakEnded(event: AdBreakEndEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
    }

}