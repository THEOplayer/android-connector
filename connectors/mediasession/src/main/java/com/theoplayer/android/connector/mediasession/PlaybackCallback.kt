package com.theoplayer.android.connector.mediasession

/**
 * PlaybackCallback allows handling playback actions when sent by a media controller.
 * By default, the connector will execute default behavior for supported actions.
 * Setting a PlaybackCallback will disable the default handling of these actions, and instead route
 * them to the callback.
 */
interface PlaybackCallback {
    /**
     * Called when a media controller wants to play or pause the current media.
     */
    fun onPlay()

    /**
     * Called when a media controller wants to pause the current media.
     */
    fun onPause()

    /**
     * Called when a media controller wants to stop the current media.
     */
    fun onStop()

    /**
     * Called when a media controller wants to fast forward the current media.
     */
    fun onFastForward()

    /**
     * Called when a media controller wants to rewind the current media.
     */
    fun onRewind()

    /**
     * Called when a media controller wants to change the playback speed of the current media.
     */
    fun onSetPlaybackSpeed(speed: Float)

    /**
     * Called when a media controller wants to seek to a specific position in the current media.
     */
    fun onSeekTo(positionMs: Long)
}