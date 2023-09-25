package com.theoplayer.android.connector.mediasession

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat

/**
 * MediaSessionListener provides the possibility to additionally listen to actions passed to the
 * player by MediaSessionCompat.Callback.
 */
abstract class MediaSessionListener {

    /**
     * Called when media session requests to begin playback.
     *
     * @see MediaSessionCompat.Callback.onPlay
     */
    open fun onPlay() {}

    /**
     * Called when media session requests to pause playback.
     *
     * @see MediaSessionCompat.Callback.onPause
     */
    open fun onPause() {}

    /**
     * Called when media session requests to stop playback.
     *
     * @see MediaSessionCompat.Callback.onStop
     */
    open fun onStop() {}

    /**
     * Called when media session requests to seek to a specific position in ms.
     *
     * @see MediaSessionCompat.Callback.onSeekTo
     */
    open fun onSeekTo(positionMs: Long) {}

    /**
     * Called when media session requests to fast forward playback.
     *
     * @see MediaSessionCompat.Callback.onFastForward
     */
    open fun onFastForward() {}

    /**
     * Called when media session requests to rewind playback.
     *
     * @see MediaSessionCompat.Callback.onRewind
     */
    open fun onRewind() {}

    /**
     * Called when media session requests to change playback speed.
     *
     * @see MediaSessionCompat.Callback.onSetPlaybackSpeed
     */
    open fun onSetPlaybackSpeed(speed: Float) {}

    /**
     * Called when media session requests to skip to the next item in the playback queue.
     *
     * @see MediaSessionCompat.Callback.onSkipToNext
     */
    open fun onSkipToNext() {}

    /**
     * Called when media session requests to skip to the previous item in the playback queue.
     *
     * @see MediaSessionCompat.Callback.onSkipToPrevious
     */
    open fun onSkipToPrevious() {}

    /**
     * Called when media session requests to skip to a specific queue item.
     *
     * @see MediaSessionCompat.Callback.onSkipToQueueItem
     */
    open fun onSkipToQueueItem(id: Long) {}

    /**
     * Called when media session requests to add an item to the playback queue.
     *
     * @see MediaSessionCompat.Callback.onAddQueueItem
     */
    open fun onAddQueueItem(description: MediaDescriptionCompat) {}

    /**
     * Called when media session requests to remove an item from the playback queue.
     *
     * @see MediaSessionCompat.Callback.onRemoveQueueItem
     */
    open fun onRemoveQueueItem(description: MediaDescriptionCompat) {}

    /**
     * Called when media session requests to execute a custom action.
     *
     * @see MediaSessionCompat.Callback.onCustomAction
     */
    open fun onCustomAction(action: String, extras: Bundle?) {}

    /**
     * Called when media session requests to se an item's rating.
     *
     * @see MediaSessionCompat.Callback.onSetRating
     */
    open fun onSetRating(rating: RatingCompat, extras: Bundle?) {}
}