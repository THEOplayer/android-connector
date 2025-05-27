package com.theoplayer.android.connector.mediasession

import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.*
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.ReadyState

class PlaybackStateProvider(private val connector: MediaSessionConnector) {
    companion object {
        const val DEFAULT_PLAYBACK_ACTIONS = (
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_FAST_FORWARD or
                        PlaybackStateCompat.ACTION_REWIND or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED)
    }

    private var player: Player? = null
    private val builder = PlaybackStateCompat.Builder()

    private val timeUpdateListener = EventListener<TimeUpdateEvent> { e -> onTimeUpdate(e) }
    private val sourceChangeListener = EventListener<SourceChangeEvent> { e -> onSourceChange(e) }
    private val loadedMetadataListener = EventListener<LoadedMetadataEvent> { e -> onLoadedMetadata(e) }
    private val playListener = EventListener<PlayEvent> { e -> onPlay(e) }
    private val playingListener = EventListener<PlayingEvent> { e -> onPlaying(e) }
    private val pauseListener = EventListener<PauseEvent> { e -> onPause(e) }
    private val errorListener = EventListener<ErrorEvent> { e -> onError(e) }
    private val waitingListener = EventListener<WaitingEvent> { e -> onWaiting(e) }
    private val endedListener = EventListener<EndedEvent> { e -> onEnded(e) }
    private val seekedListener = EventListener<SeekedEvent> { e -> onSeeked(e) }
    private val durationChangeListener = EventListener<DurationChangeEvent> { e -> onDurationChange(e) }

    @PlaybackStateCompat.State
    private var playbackState = PlaybackStateCompat.STATE_NONE

    fun setPlayer(player: Player?) {
        if (this.player != null) {
            unregisterListeners()
        }
        this.player = player
        registerListeners()
        updatePlaybackState(PlaybackStateCompat.STATE_NONE)
    }

    fun destroy() {
        unregisterListeners()
    }

    private fun registerListeners() {
        player?.apply {
            addEventListener(PlayerEventTypes.TIMEUPDATE, timeUpdateListener)
            addEventListener(PlayerEventTypes.SOURCECHANGE, sourceChangeListener)
            addEventListener(PlayerEventTypes.LOADEDMETADATA, loadedMetadataListener)
            addEventListener(PlayerEventTypes.PLAY, playListener)
            addEventListener(PlayerEventTypes.PLAYING, playingListener)
            addEventListener(PlayerEventTypes.PAUSE, pauseListener)
            addEventListener(PlayerEventTypes.ERROR, errorListener)
            addEventListener(PlayerEventTypes.WAITING, waitingListener)
            addEventListener(PlayerEventTypes.ENDED, endedListener)
            addEventListener(PlayerEventTypes.SEEKED, seekedListener)
            addEventListener(PlayerEventTypes.DURATIONCHANGE, durationChangeListener)
        }
    }

    private fun unregisterListeners() {
        player?.apply {
            removeEventListener(PlayerEventTypes.TIMEUPDATE, timeUpdateListener)
            removeEventListener(PlayerEventTypes.SOURCECHANGE, sourceChangeListener)
            removeEventListener(PlayerEventTypes.LOADEDMETADATA, loadedMetadataListener)
            removeEventListener(PlayerEventTypes.PLAY, playListener)
            removeEventListener(PlayerEventTypes.PLAYING, playingListener)
            removeEventListener(PlayerEventTypes.PAUSE, pauseListener)
            removeEventListener(PlayerEventTypes.ERROR, errorListener)
            removeEventListener(PlayerEventTypes.WAITING, waitingListener)
            removeEventListener(PlayerEventTypes.ENDED, endedListener)
            removeEventListener(PlayerEventTypes.SEEKED, seekedListener)
            removeEventListener(PlayerEventTypes.DURATIONCHANGE, durationChangeListener)
        }
    }

    private val onTimeUpdate = { _: TimeUpdateEvent ->
        if (connector.shouldDispatchTimeUpdateEvents) {
            invalidatePlaybackState()
        }
    }

    private val onSourceChange = { _: SourceChangeEvent ->
        if (player?.source != null) {
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        } else {
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        }
        connector.setMediaSessionMetadata(player?.source)
    }

    private val onLoadedMetadata = { _: LoadedMetadataEvent ->
        connector.invalidateMediaSessionMetadata()
    }

    private val onPlay = { _: PlayEvent ->
        if (player!!.readyState.ordinal < ReadyState.HAVE_CURRENT_DATA.ordinal) {
            updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        }
    }

    private val onPlaying = { _: PlayingEvent ->
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    private val onPause = { _: PauseEvent ->
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }

    private val onError = { _: ErrorEvent ->
        updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
    }

    private val onWaiting = { _: WaitingEvent ->
        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING)
    }

    private val onEnded = { _: EndedEvent ->
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }

    private val onSeeked = { _: SeekedEvent ->
        // Some clients listening to the mediaSession, such as Notifications, do not update the
        // currentTime until playbackState becomes PLAYING, so force it.
        val oldPlaybackState = playbackState
        playbackState = PlaybackStateCompat.STATE_PLAYING
        invalidatePlaybackState()
        playbackState = oldPlaybackState
        invalidatePlaybackState()
    }

    private val onDurationChange = { _: DurationChangeEvent ->
        connector.invalidateMediaSessionMetadata()
    }

    private fun buildPrepareActions(): Long {
        val playbackPreparer = connector.playbackPreparer
        return if (playbackPreparer == null) 0 else PlaybackPreparer.AVAILABLE_ACTIONS and playbackPreparer.getSupportedPrepareActions()
    }

    private fun buildPlaybackActions(): Long {
        var playbackActions = DEFAULT_PLAYBACK_ACTIONS

        // Optionally add queueNavigator actions.
        val queueNavigator = connector.queueNavigator
        if (queueNavigator != null) {
            playbackActions =
                playbackActions or (QueueNavigator.AVAILABLE_ACTIONS and queueNavigator.getSupportedQueueNavigatorActions(
                    player!!
                ))
        }
        return playbackActions
    }

    fun updatePlaybackState(newPlaybackState: Int) {
        val oldPlaybackState = playbackState
        if (oldPlaybackState == newPlaybackState) {
            return
        }
        playbackState = newPlaybackState
        invalidatePlaybackState()
    }

    fun invalidatePlaybackState() {
        var sessionPlaybackState = PlaybackStateCompat.STATE_NONE
        var position = 0L
        var playbackSpeed = 0f
        if (player == null) {
            builder
                .setActions(buildPrepareActions())
                .setState(
                    sessionPlaybackState,
                    position,
                    playbackSpeed,
                    SystemClock.elapsedRealtime()
                )
        } else {
            val reportError = !TextUtils.isEmpty(player!!.error)
            sessionPlaybackState =
                if (reportError) {
                    PlaybackStateCompat.STATE_ERROR
                } else {
                    playbackState
                }
            if (reportError) {
                builder.setErrorMessage(
                    PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR,
                    player!!.error
                )
            }
            val queueNavigator = connector.queueNavigator
            val timeRanges = player!!.buffered
            if (timeRanges.length() > 0) {
                builder.setBufferedPosition((1e03 * timeRanges.getEnd(timeRanges.length() - 1)).toLong())
            }
            val activeQueueItemId = queueNavigator?.getActiveQueueItemId(player!!)
                ?: MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()
            position = (1e03 * player!!.currentTime).toLong()
            playbackSpeed = player!!.playbackRate.toFloat()
            builder.setActions(buildPrepareActions() or buildPlaybackActions())
            builder.setActiveQueueItemId(activeQueueItemId)
            builder.setState(
                sessionPlaybackState,
                position,
                playbackSpeed,
                SystemClock.elapsedRealtime()
            )
        }

        with(connector.mediaSession) {
            setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
            setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
            setPlaybackState(builder.build())
        }

        if (connector.debug) {
            Log.d(
                TAG, "PlaybackStateProvider::updatePlaybackState " +
                        "playbackState: ${playbackStateToString(sessionPlaybackState)}; " +
                        "position: $position; " +
                        "playbackSpeed: $playbackSpeed; "
            )
        }
    }

    private fun playbackStateToString(state: Int): String {
        return when (state) {
            PlaybackStateCompat.STATE_NONE -> "STATE_NONE"
            PlaybackStateCompat.STATE_STOPPED -> "STATE_STOPPED"
            PlaybackStateCompat.STATE_PAUSED -> "STATE_PAUSED"
            PlaybackStateCompat.STATE_PLAYING -> "STATE_PLAYING"
            PlaybackStateCompat.STATE_FAST_FORWARDING -> "STATE_FAST_FORWARDING"
            PlaybackStateCompat.STATE_REWINDING -> "STATE_REWINDING"
            PlaybackStateCompat.STATE_BUFFERING -> "STATE_BUFFERING"
            PlaybackStateCompat.STATE_ERROR -> "STATE_ERROR"
            PlaybackStateCompat.STATE_CONNECTING -> "STATE_CONNECTING"
            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> "STATE_SKIPPING_TO_PREVIOUS"
            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> "STATE_SKIPPING_TO_NEXT"
            PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> "STATE_SKIPPING_TO_QUEUE_ITEM"
            else -> "STATE_UNKNOWN(${state})"
        }
    }
}
