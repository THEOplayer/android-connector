@file:Suppress("unused")

package com.theoplayer.android.connector.mediasession

import android.media.session.PlaybackState.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription

const val TAG = "MediaSessionConnector"
const val DEFAULT_MEDIA_SESSION_FLAGS = (MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
        or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
const val EDITOR_MEDIA_SESSION_FLAGS =
    DEFAULT_MEDIA_SESSION_FLAGS or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS

const val DEFAULT_SKIP_FORWARD_INTERVAL = 5.0
const val DEFAULT_SKIP_BACKWARDS_INTERVAL = 5.0

/**
 * MediaSessionConnector connects an Android media session to a THEOplayer instance.
 *
 * <pre>
 *   // Create and initialize the media session
 *   mediaSession = MediaSessionCompat(this, TAG).apply {
 *     // Do not let MediaButtons restart the player when the app is not visible
 *     setMediaButtonReceiver(null)
 *   }
 *
 *   // Create a MediaSessionConnector and attach the THEOplayer instance.
 *   mediaSessionConnector = MediaSessionConnector(mediaSession)
 *   mediaSessionConnector.player = player
 *
 *   // Optionally show debug logs
 *   mediaSessionConnector.debug = true
 *
 *   // Set mediaSession to active
 *   mediaSessionConnector.setActive(true)
 * </pre>
 *
 * @see
 *  <ul>
 *      <li><a href="https://developer.android.com/guide/topics/media-apps/working-with-a-media-session">Using a media session</a></li>
 *      <li><a href="https://developer.android.com/guide/topics/media-apps/video-app/building-a-video-app">Building a video app</a></li>
 *      <li><a href="https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/ext/mediasession/MediaSessionConnector.html">ExoPlayer MediaSessionConnector</a></li>
 *  </ul>
 */
class MediaSessionConnector(val mediaSession: MediaSessionCompat) {
    private val mediaSessionCallback: MediaSessionCallback = MediaSessionCallback(this)
    private val metadataProvider: MediaMetadataProvider = MediaMetadataProvider(this)
    private val playbackStateProvider: PlaybackStateProvider = PlaybackStateProvider(this)
    private var destroyed = false

    val listeners: MutableList<MediaSessionListener> = mutableListOf()

    var player: Player? = null
        set(value) {
            if (field != null) {
                playbackStateProvider.destroy()
            }
            field = value
            playbackStateProvider.setPlayer(field)

            if (player?.source == null) {
                metadataProvider.clearMediaSessionMetadataDescription()
                playbackStateProvider.updatePlaybackState(STATE_NONE)
            } else {
                metadataProvider.setMediaSessionMetadata(player?.source)
                playbackStateProvider.updatePlaybackState(
                    when (player?.isPaused) {
                        false -> STATE_PLAYING
                        true -> STATE_PAUSED
                        else -> STATE_NONE
                    }
                )
            }
        }

    var queueNavigator: QueueNavigator? = null
    var ratingCallback: RatingCallback? = null
    var playbackPreparer: PlaybackPreparer? = null
    var queueEditor: QueueEditor? = null
        set(value) {
            field = value
            mediaSession.setFlags(
                if (value == null)
                    DEFAULT_MEDIA_SESSION_FLAGS
                else
                    EDITOR_MEDIA_SESSION_FLAGS
            )
        }

    var shouldDispatchUnsupportedActions: Boolean = false

    /**
     * Whether each time update event should trigger an update in playback state.
     */
    var shouldDispatchTimeUpdateEvents: Boolean = false

    var debug: Boolean = BuildConfig.DEBUG
    var enabledPlaybackActions: Long = PlaybackStateProvider.DEFAULT_PLAYBACK_ACTIONS
    var customActionProviders: Array<CustomActionProvider> = arrayOf()

    /**
     * The interval the player should skip forward when fast-forwarding, in seconds.
     */
    var skipForwardInterval = DEFAULT_SKIP_FORWARD_INTERVAL

    /**
     * The interval the player should skip backward when rewinding, in seconds.
     */
    var skipBackwardsInterval = DEFAULT_SKIP_BACKWARDS_INTERVAL

    init {
        if (debug) {
            Log.d(TAG, "Connector initialized")
        }
    }

    /**
     * Pass a custom description for the current media item.
     */
    fun setMediaSessionMetadata(sourceDescription: SourceDescription?) {
        metadataProvider.setMediaSessionMetadata(sourceDescription)
    }

    /**
     * Get metadata for the current media item.
     */
    fun getMediaSessionMetadata(): MediaMetadataCompat {
        return metadataProvider.getMediaSessionMetadata()
    }

    /**
     * Get metadata provider for the current media item.
     */
    fun getMediaSessionMetadataProvider(): MediaMetadataProvider {
        return metadataProvider
    }

    /**
     * Force update of the current meta data.
     */
    fun invalidateMediaSessionMetadata() {
        metadataProvider.invalidateMediaSessionMetadata()
    }

    /**
     * Force update of current playback state.
     */
    fun invalidatePlaybackState() {
        playbackStateProvider.invalidatePlaybackState()
    }

    /**
     * Set mediaSession active flag.
     */
    fun setActive(active: Boolean) {
        if (mediaSession.isActive == active) {
            return
        }

        mediaSession.isActive = active

        // If mediaSession.isActive is `false`, mediaSession will still receive MediaSessionCallback
        // events, so drop the callback when inactive.
        mediaSession.setCallback(if (active) mediaSessionCallback else null)

        if (debug) {
            Log.d(TAG, "MediaSession setActive: $active")
        }
    }

    /**
     * Add a listener for media session callback actions.
     */
    fun addListener(listener: MediaSessionListener) {
        listeners.add(listener)
    }

    /**
     * Remove a listener for media session callback actions.
     */
    fun removeListener(listener: MediaSessionListener) {
        listeners.remove(listener)
    }

    /**
     * Release mediaSession.
     */
    fun destroy() {
        if (destroyed) {
            return
        }
        destroyed = true
        playbackStateProvider.destroy()
        mediaSession.release()
        listeners.clear()
        if (debug) {
            Log.d(TAG, "MediaSession released")
        }
    }
 }
