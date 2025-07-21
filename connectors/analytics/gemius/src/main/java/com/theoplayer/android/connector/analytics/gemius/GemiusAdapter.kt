package com.theoplayer.android.connector.analytics.gemius

import android.content.Context
import android.util.Log
import com.gemius.sdk.stream.AdData
import com.gemius.sdk.stream.EventAdData
import com.gemius.sdk.stream.EventProgramData
import com.theoplayer.android.api.THEOplayerView
import com.gemius.sdk.stream.Player
import com.gemius.sdk.stream.PlayerData
import com.gemius.sdk.stream.ProgramData
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.LinearAd
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
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.event.player.SeekingEvent
import com.theoplayer.android.api.event.player.SourceChangeEvent
import com.theoplayer.android.api.event.player.VolumeChangeEvent
import com.theoplayer.android.api.event.player.WaitingEvent
import com.theoplayer.android.api.event.track.mediatrack.audio.QualityChangedEvent
import com.theoplayer.android.api.event.track.mediatrack.video.VideoTrackEventTypes
import com.theoplayer.android.api.event.track.mediatrack.video.list.AddTrackEvent
import com.theoplayer.android.api.event.track.mediatrack.video.list.RemoveTrackEvent
import com.theoplayer.android.api.event.track.mediatrack.video.list.VideoTrackListEventTypes
import com.theoplayer.android.api.player.track.mediatrack.quality.VideoQuality

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

    private val onAdBreakBegin: EventListener<AdBreakBeginEvent>
    private val onAdBegin: EventListener<AdBeginEvent>
    private val onAdEnd: EventListener<AdEndEvent>
    private val onAdSkip: EventListener<AdSkipEvent>
    private val onAdBreakEnded: EventListener<AdBreakEndEvent>

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
        onAdBreakBegin = EventListener { event -> handleAdBreakBegin(event) }
        onAdBegin = EventListener { event -> handleAdBegin(event) }
        onAdEnd = EventListener { event -> handleAdEnd(event) }
        onAdSkip = EventListener { event -> handleAdSkip(event) }
        onAdBreakEnded = EventListener { event -> handleAdBreakEnded(event) }

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
        playerView.player.ads.addEventListener(AdsEventTypes.AD_BREAK_BEGIN, onAdBreakBegin)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_BEGIN, onAdBegin)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_END, onAdEnd)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_SKIP, onAdSkip)
        playerView.player.ads.addEventListener(AdsEventTypes.AD_BREAK_END, onAdBreakEnded)
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
            if (hasPrerollScheduled()) return
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
            Log.d(TAG, "Player Event: ${event.type}: currentTime = ${event.currentTime}")
        }
        reportBasicEvent(Player.EventType.PAUSE)
    }
    private fun handleWaiting(event: WaitingEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: currentTime = ${event.currentTime}")
        }
        reportBasicEvent(Player.EventType.BUFFER)
    }
    private fun handleSeeking(event: SeekingEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: currentTime = ${event.currentTime}")
        }
        reportBasicEvent(Player.EventType.SEEK)
    }
    private fun handleError(event: ErrorEvent) {
        if (configuration.debug) {
            val errorObject = event.errorObject
            Log.d(TAG, "Player Event: ${event.type}: error = ${errorObject.code}: ${errorObject.message}")
        }
        reportBasicEvent(Player.EventType.COMPLETE)
    }
    private fun handleEnded(event: EndedEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: currentTime = ${event.currentTime}")
        }
        reportBasicEvent(Player.EventType.COMPLETE)
    }
    private fun handleVolumeChange(event: VolumeChangeEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: volume = ${event.volume}")
        }
        val computedVolume = computeVolume()
        val programId = programId ?: return
        currentAd?.let { ad ->
            val adBreak = ad.adBreak ?: return
            val adEventData = EventAdData()
            adEventData.volume = computedVolume
            gemiusPlayer?.adEvent(programId, ad.id, adBreak.timeOffset, Player.EventType.CHANGE_VOL, adEventData)
        } ?: run {
            val programEventData = EventProgramData()
            programEventData.volume = computedVolume
            gemiusPlayer?.programEvent(programId, playerView.player.currentTime.toInt(), Player.EventType.CHANGE_VOL, programEventData)
        }
    }
    private fun handleAddVideoTrack(event: AddTrackEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event (Video Track): ${event.type}")
        }
        val track = event.track
        track.addEventListener(VideoTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT, onVideoQualityChanged)
    }
    private fun handleRemoveVideoTrack(event: RemoveTrackEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
        val track = event.track
        track.removeEventListener(VideoTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT, onVideoQualityChanged)
    }
    private fun handleVideoQualityChanged(event: QualityChangedEvent<*,*>) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}")
        }
        val programId = programId ?: return
        val activeQuality = (event.getQuality() as VideoQuality)
        val height = activeQuality.height
        val width = activeQuality.width
        currentAd?.let { ad ->
            val adBreak = ad.adBreak ?: return
            val adEventData = EventAdData()
            adEventData.quality = "${width}x${height}"
            gemiusPlayer?.adEvent(programId,ad.id,adBreak.timeOffset,Player.EventType.CHANGE_QUAL,adEventData)
        } ?: run {
            val programEventData = EventProgramData()
            programEventData.quality = "${width}x${height}"
            gemiusPlayer?.programEvent(programId,playerView.player.currentTime.toInt(),Player.EventType.CHANGE_QUAL,programEventData)
        }
    }
    private fun handleAdBreakBegin(event: AdBreakBeginEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: offset = ${event.adBreak.timeOffset}")
        }
        reportBasicEvent(Player.EventType.BREAK)
        playerView.player.removeEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
        playerView.player.addEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
    }
    private fun handleAdBegin(event: AdBeginEvent) {
        val ad = event.ad
        currentAd = ad
        val adId = ad?.id ?: return
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: id = $adId")
        }
        val adData = buildAdData(ad)
        gemiusPlayer?.newAd(adId,adData)
    }
    private fun handleAdEnd(event: AdEndEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: id = ${event.ad?.id}")
        }
        reportBasicEvent(Player.EventType.COMPLETE)
        reportBasicEvent(Player.EventType.CLOSE)
        adCount++
        currentAd = null
        playerView.player.removeEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
        playerView.player.addEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
    }
    private fun handleAdSkip(event: AdSkipEvent) {
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: id = ${event.ad?.id}")
        }
        reportBasicEvent(Player.EventType.SKIP)
    }
    private fun handleAdBreakEnded(event: AdBreakEndEvent) {
        val offset = event.adBreak.timeOffset
        if (configuration.debug) {
            Log.d(TAG, "Player Event: ${event.type}: offset = $offset")
        }
        adCount = 1
        if (offset > 0) partCount++
        val programId = programId ?: return
        val programData = programData ?: return
        gemiusPlayer?.newProgram(programId, programData)
        playerView.player.removeEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
        if (offset == 0) playerView.player.addEventListener(PlayerEventTypes.PLAYING, onFirstPlaying)
    }
    private fun reportBasicEvent(eventType: Player.EventType) {
        val programId = programId ?: return
        currentAd?.let { ad ->
            val offset = ad.adBreak?.timeOffset ?: return
            // docs mention null can be passed but interface prohibits
            gemiusPlayer?.adEvent(programId,ad.id, offset, eventType, EventAdData())
        } ?: run {
            // docs mention null can be passed but interface prohibits
            gemiusPlayer?.programEvent(programId, playerView.player.currentTime.toInt(), eventType, EventProgramData())
        }
    }

    private fun buildAdData(ad: Ad): AdData {
        val adData = AdData()
        val linearAd = ad as? LinearAd ?: return adData
        adProcessor?.let { adProcessor ->
            return adProcessor.apply(ad)
        } ?: run {
            adData.name = linearAd.id
            adData.adType = AdData.AdType.BREAK
            adData.adFormat = 1 // 1 = VIDEO ; 2 = AUDIO
            adData.duration = linearAd.duration
            adData.quality = "${playerView.player.videoWidth}x${playerView.player.videoHeight}"
            adData.resolution = "${playerView.width}x${playerView.height}"
            return adData
        }
    }

    private fun hasPrerollScheduled(): Boolean {
        return playerView.player.ads.scheduledAds.any { it.adBreak?.timeOffset == 0 }
    }
}