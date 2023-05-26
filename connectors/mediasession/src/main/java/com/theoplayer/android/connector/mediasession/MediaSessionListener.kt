package com.theoplayer.android.connector.mediasession

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.RatingCompat

abstract class MediaSessionListener {

    open fun onPlay() {}

    open fun onPause() {}

    open fun onStop() {}

    open fun onSeekTo(positionMs: Long) {}

    open fun onFastForward() {}

    open fun onRewind() {}

    open fun onSetPlaybackSpeed(speed: Float) {}

    open fun onSkipToNext() {}

    open fun onSkipToPrevious() {}

    open fun onSkipToQueueItem(id: Long) {}

    open fun onAddQueueItem(description: MediaDescriptionCompat) {}

    open fun onRemoveQueueItem(description: MediaDescriptionCompat) {}

    open fun onCustomAction(action: String, extras: Bundle?) {}

    open fun onSetRating(rating: RatingCompat, extras: Bundle?) {}
}