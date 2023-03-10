package com.theoplayer.android.connector.mediasession

import android.media.session.PlaybackState.STATE_NONE
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

    var player: Player? = null
        set(value) {
            if (field != null) {
                playbackStateProvider.destroy()
            }
            field = value
            playbackStateProvider.setPlayer(field)

            metadataProvider.clearMediaSessionMetadataDescription()
            playbackStateProvider.updatePlaybackState(STATE_NONE)
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
     * Whether each timeupdate event should trigger an update in playback state.
     */
    var shouldDispatchTimeUpdateEvents: Boolean = false

    var debug: Boolean = BuildConfig.DEBUG
    var enabledPlaybackActions: Long = PlaybackStateProvider.DEFAULT_PLAYBACK_ACTIONS
    var customActionProviders: Array<CustomActionProvider> = arrayOf()

    init {
        mediaSession.setCallback(mediaSessionCallback)
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
        if (debug) {
            Log.d(TAG, "MediaSession setActive: $active")
        }
    }

    /**
     * Release mediaSession.
     */
    fun destroy() {
        mediaSession.release()
        if (debug) {
            Log.d(TAG, "MediaSession released")
        }
    }
 }
