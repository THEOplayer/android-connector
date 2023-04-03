package com.theoplayer.android.connector.mediasession

abstract class MediaSessionListener {

    open fun onPlay() {}

    open fun onPause() {}

    open fun onStop() {}

    open fun onSeekTo(positionMs: Long) {}
}