package com.theoplayer.android.connector.yospace.internal

import android.util.Log
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.EndedEvent
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.event.player.SeekedEvent
import com.theoplayer.android.api.event.player.VolumeChangeEvent
import com.theoplayer.android.api.event.player.WaitingEvent
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.connector.yospace.TAG
import com.theoplayer.android.connector.yospace.USER_AGENT
import com.theoplayer.android.connector.yospace.YospaceListener
import com.theoplayer.android.connector.yospace.YospaceSsaiDescription
import com.theoplayer.android.connector.yospace.YospaceStreamType
import com.yospace.admanagement.AnalyticEventObserver
import com.yospace.admanagement.PlaybackEventHandler
import com.yospace.admanagement.Session
import com.yospace.admanagement.SessionDVRLive
import com.yospace.admanagement.SessionLive
import com.yospace.admanagement.SessionVOD
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class YospaceAdIntegration(
    private val player: Player,
    private val controller: ServerSideAdIntegrationController,
    private val analyticEventObserver: AnalyticEventObserver,
    private val listener: YospaceListener
) : ServerSideAdIntegrationHandler {
    private var session: Session? = null
    private var timedMetadataHandler: TimedMetadataHandler? = null
    private var didFirstPlay: Boolean = false
    private var isMuted: Boolean = false
    private var isStalling: Boolean = false

    private val currentPlayhead: Long
        get() = (player.currentTime * 1000.0).toLong()

    override suspend fun setSource(source: SourceDescription): SourceDescription {
        val yospaceSource = source.sources.find { it.ssai is YospaceSsaiDescription } ?: return source
        val ssaiDescription = yospaceSource.ssai as? YospaceSsaiDescription ?: return source

        // Create the Yospace session
        val src = yospaceSource.src
        val sessionProperties = ssaiDescription.sessionProperties.copy(
            userAgent = ssaiDescription.sessionProperties.userAgent.ifEmpty { USER_AGENT }
        )
        val session = when (ssaiDescription.streamType) {
            YospaceStreamType.LIVE -> createSessionLive(src, sessionProperties)

            YospaceStreamType.NONLINEAR,
            YospaceStreamType.LIVEPAUSE -> createSessionDVRLive(src, sessionProperties)

            YospaceStreamType.VOD -> createSessionVOD(src, sessionProperties)
        }

        when (session.sessionState) {
            Session.SessionState.INITIALISED,
            Session.SessionState.NO_ANALYTICS -> {
                // Set up
                setupSession(session)
                // Notify listener
                listener.onSessionAvailable()
                // Replace source with playback URL
                val newSource = source.replaceSources(source.sources.toMutableList().apply {
                    remove(yospaceSource)
                    add(0, yospaceSource.replaceSrc(session.playbackUrl))
                })
                return newSource
            }

            Session.SessionState.FAILED -> {
                val resultCode = session.resultCode
                session.shutdown()
                throw Exception(getSessionErrorMessage(resultCode))
            }

            Session.SessionState.SHUTDOWN -> {
                // Already shutdown, ignore
            }

            else -> {
                Log.d(TAG, "Unexpected Yospace session state: ${session.sessionState}")
                session.shutdown()
            }
        }
        return source
    }

    private fun setupSession(session: Session) {
        this.session = session
        session.apply {
            addAnalyticObserver(analyticEventObserver)
        }
        addPlayerListeners()
        timedMetadataHandler = TimedMetadataHandler(player, session)
    }

    private fun destroySession() {
        removePlayerListeners()
        timedMetadataHandler?.apply {
            destroy()
            timedMetadataHandler = null
        }
        session?.apply {
            removeAnalyticObserver(analyticEventObserver)
            shutdown()
            session = null
        }
    }

    private fun addPlayerListeners() {
        player.addEventListener(PlayerEventTypes.VOLUMECHANGE, onVolumeChange)
        player.addEventListener(PlayerEventTypes.PLAY, onPlay)
        player.addEventListener(PlayerEventTypes.ENDED, onEnded)
        player.addEventListener(PlayerEventTypes.PAUSE, onPause)
        player.addEventListener(PlayerEventTypes.SEEKED, onSeeked)
        player.addEventListener(PlayerEventTypes.WAITING, onWaiting)
        player.addEventListener(PlayerEventTypes.PLAYING, onPlaying)
    }

    private fun removePlayerListeners() {
        player.removeEventListener(PlayerEventTypes.VOLUMECHANGE, onVolumeChange)
        player.removeEventListener(PlayerEventTypes.PLAY, onPlay)
        player.removeEventListener(PlayerEventTypes.ENDED, onEnded)
        player.removeEventListener(PlayerEventTypes.PAUSE, onPause)
        player.removeEventListener(PlayerEventTypes.SEEKED, onSeeked)
        player.removeEventListener(PlayerEventTypes.WAITING, onWaiting)
        player.removeEventListener(PlayerEventTypes.PLAYING, onPlaying)
    }

    private val onVolumeChange = EventListener<VolumeChangeEvent> {
        val muted = player.isMuted
        if (isMuted != muted) {
            isMuted = muted
            session?.onVolumeChange(muted)
        }
    }

    private val onPlay = EventListener<PlayEvent> {
        if (!didFirstPlay) {
            didFirstPlay = true
            session?.onPlayerEvent(PlaybackEventHandler.PlayerEvent.START, currentPlayhead)
        } else {
            session?.onPlayerEvent(PlaybackEventHandler.PlayerEvent.RESUME, currentPlayhead)
        }
    }

    private val onEnded = EventListener<EndedEvent> {
        session?.onPlayerEvent(PlaybackEventHandler.PlayerEvent.STOP, currentPlayhead)
    }

    private val onPause = EventListener<PauseEvent> {
        session?.onPlayerEvent(PlaybackEventHandler.PlayerEvent.PAUSE, currentPlayhead)
    }

    private val onSeeked = EventListener<SeekedEvent> {
        session?.onPlayerEvent(PlaybackEventHandler.PlayerEvent.SEEK, currentPlayhead)
    }

    private val onWaiting = EventListener<WaitingEvent> {
        isStalling = true
        session?.onPlayerEvent(PlaybackEventHandler.PlayerEvent.STALL, currentPlayhead)
    }

    private val onPlaying = EventListener<PlayingEvent> {
        if (isStalling) {
            isStalling = false
            session?.onPlayerEvent(PlaybackEventHandler.PlayerEvent.CONTINUE, currentPlayhead)
        }
    }

    override suspend fun resetSource() {
        destroySession()
        didFirstPlay = false
        isStalling = false
    }

    override suspend fun destroy() {
        resetSource()
    }
}

private suspend fun createSessionLive(url: String, properties: Session.SessionProperties?) = suspendCoroutine { continuation ->
    SessionLive.create(url, properties) { event -> continuation.resume(event.payload) }
}

private suspend fun createSessionDVRLive(url: String, properties: Session.SessionProperties?) = suspendCoroutine { continuation ->
    SessionDVRLive.create(url, properties) { event -> continuation.resume(event.payload) }
}

private suspend fun createSessionVOD(url: String, properties: Session.SessionProperties?) = suspendCoroutine { continuation ->
    SessionVOD.create(url, properties) { event -> continuation.resume(event.payload) }
}

private fun getSessionErrorMessage(resultCode: Int): String {
    val message = when (resultCode) {
        YospaceSessionResultCode.MALFORMED_URL -> "The stream URL is not correctly formatted"
        YospaceSessionResultCode.CONNECTION_ERROR -> "Connection error"
        YospaceSessionResultCode.CONNECTION_TIMEOUT -> "Connection timeout"
        else -> "Session could not be initialised"
    }
    return "Yospace: $message (code = $resultCode)"
}