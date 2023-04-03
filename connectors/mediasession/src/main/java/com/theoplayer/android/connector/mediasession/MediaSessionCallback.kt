package com.theoplayer.android.connector.mediasession

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.util.Log
import com.theoplayer.android.api.timerange.TimeRanges

class MediaSessionCallback(private val connector: MediaSessionConnector) :
    MediaSessionCompat.Callback() {

    companion object {
        private const val DEFAULT_SKIP_TIME = 5.0 // 5s
    }

    override fun onPrepare() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onPrepare")
        }
        if (shouldHandlePlaybackPreparerAction(ACTION_PREPARE)) {
            connector.playbackPreparer?.onPrepare(false)
        }
    }

    override fun onPrepareFromMediaId(mediaId: String, extras: Bundle?) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onPrepareFromMediaId $mediaId")
        }
        if (shouldHandlePlaybackPreparerAction(ACTION_PREPARE_FROM_MEDIA_ID)) {
            connector.playbackPreparer?.onPrepareFromMediaId(mediaId, false, extras)
        }
    }

    override fun onPrepareFromSearch(query: String, extras: Bundle?) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onPrepareFromSearch $query")
        }
        if (shouldHandlePlaybackPreparerAction(ACTION_PREPARE_FROM_SEARCH)) {
            connector.playbackPreparer?.onPrepareFromSearch(query, false, extras)
        }
    }

    override fun onPrepareFromUri(uri: Uri, extras: Bundle?) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onPrepareFromUri ${uri.path}")
        }
        if (shouldHandlePlaybackPreparerAction(ACTION_PREPARE_FROM_URI)) {
            connector.playbackPreparer?.onPrepareFromUri(uri, false, extras)
        }
    }

    override fun onPlay() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onPlay")
        }
        if (shouldHandlePlaybackAction(ACTION_PLAY)) {
            connector.player?.play()
            connector.listeners.forEach { listener ->
                listener.onPlay()
            }

            // Make sure the session is currently active and ready to receive commands.
            connector.setActive(true)
        }
    }

    override fun onPause() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onPause")
        }
        if (shouldHandlePlaybackAction(ACTION_PAUSE)) {
            connector.player?.pause()
            connector.listeners.forEach { listener ->
                listener.onPause()
            }
        }
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onPlayFromMediaId $mediaId")
        }
        if (shouldHandlePlaybackPreparerAction(ACTION_PREPARE_FROM_MEDIA_ID)) {
            connector.playbackPreparer?.onPrepareFromMediaId(mediaId, true, extras)
        }
    }

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onPlayFromSearch $query")
        }
        if (shouldHandlePlaybackPreparerAction(ACTION_PREPARE_FROM_SEARCH)) {
            connector.playbackPreparer?.onPrepareFromSearch(query, true, extras)
        }
    }

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onPlayFromUri ${uri?.path}")
        }
        if (shouldHandlePlaybackPreparerAction(ACTION_PREPARE_FROM_URI)) {
            connector.playbackPreparer?.onPrepareFromUri(uri, true, extras)
        }
    }

    override fun onStop() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onStop")
        }
        if (shouldHandlePlaybackAction(ACTION_STOP)) {
//            connector.player?.stop()
//            connector.setActive(false)
            connector.listeners.forEach { listener ->
                listener.onStop()
            }
        }
    }

    override fun onFastForward() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onFastForward")
        }
        if (shouldHandlePlaybackAction(ACTION_FAST_FORWARD)) {
            skip(DEFAULT_SKIP_TIME)
        }
    }

    override fun onRewind() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onRewind")
        }
        if (shouldHandlePlaybackAction(ACTION_REWIND)) {
            skip(-DEFAULT_SKIP_TIME)
        }
    }

    override fun onSetShuffleMode(@ShuffleMode shuffleMode: Int) {
        // Unsupported.
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSetShuffleMode $shuffleMode")
        }
    }

    override fun onSetRepeatMode(@RepeatMode mediaSessionRepeatMode: Int) {
        // Unsupported.
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSetRepeatMode $mediaSessionRepeatMode")
        }
    }

    override fun onSetPlaybackSpeed(speed: Float) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSetPlaybackSpeed $speed")
        }
        if (shouldHandlePlaybackAction(ACTION_SET_PLAYBACK_SPEED)) {
            connector.player?.playbackRate = speed.toDouble()
        }
    }

    override fun onSkipToNext() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSkipToNext")
        }
        if (shouldHandleQueueNavigatorAction(ACTION_SKIP_TO_NEXT)) {
            connector.queueNavigator?.onSkipToNext(connector.player!!)
        }
    }

    override fun onSkipToPrevious() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSkipToPrevious")
        }
        if (shouldHandleQueueNavigatorAction(ACTION_SKIP_TO_PREVIOUS)) {
            connector.queueNavigator?.onSkipToPrevious(connector.player!!)
        }
    }

    override fun onSkipToQueueItem(id: Long) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSkipToQueueItem")
        }
        if (shouldHandleQueueNavigatorAction(ACTION_SKIP_TO_QUEUE_ITEM)) {
            connector.queueNavigator?.onSkipToQueueItem(connector.player!!, id)
        }
    }

    override fun onAddQueueItem(description: MediaDescriptionCompat) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onAddQueueItem")
        }
        if (shouldHandleQueueEditAction()) {
            connector.queueEditor?.onAddQueueItem(connector.player!!, description)
        }
    }

    override fun onAddQueueItem(description: MediaDescriptionCompat, index: Int) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onAddQueueItem")
        }
        if (shouldHandleQueueEditAction()) {
            connector.queueEditor?.onAddQueueItem(connector.player!!, description, index)
        }
    }

    override fun onRemoveQueueItem(description: MediaDescriptionCompat) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onRemoveQueueItem")
        }
        if (shouldHandleQueueEditAction()) {
            connector.queueEditor?.onRemoveQueueItem(connector.player!!, description)
        }
    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onCustomAction $action")
        }
        if (connector.player != null) {
            val player = connector.player!!
            for (customActionProvider in connector.customActionProviders) {
                if (customActionProvider.getCustomAction(player)?.action == action) {
                    customActionProvider.onCustomAction(player, action, extras)
                }
            }
        }
    }

    override fun onSeekTo(positionMs: Long) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSeekTo $positionMs")
        }
        if (shouldHandlePlaybackAction(ACTION_SEEK_TO)) {
            connector.player?.currentTime = 1e-03 * positionMs
            connector.listeners.forEach { listener ->
                listener.onSeekTo(positionMs)
            }
        }
    }

    override fun onSetRating(rating: RatingCompat) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSetRating $rating")
        }
        if (shouldHandleRatingAction()) {
            connector.ratingCallback?.onSetRating(connector.player!!, rating)
        }
    }

    override fun onSetRating(rating: RatingCompat, extras: Bundle?) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSetRating $rating")
        }
        if (shouldHandleRatingAction()) {
            connector.ratingCallback?.onSetRating(connector.player!!, rating, extras)
        }
    }

    private fun skip(skipTime: Double) {
        val player = connector.player ?: return

        val currentTime: Double = player.currentTime
        val seekable: TimeRanges = player.seekable
        if (java.lang.Double.isNaN(currentTime) || seekable.length() == 0) {
            return
        }
        for (i in 0 until seekable.length()) {
            if (seekable.getStart(i) <= currentTime && seekable.getEnd(i) >= currentTime) {
                val time = seekable.getEnd(i)
                    .coerceAtMost(seekable.getStart(i).coerceAtLeast(currentTime + skipTime))
                player.currentTime = time
            }
        }
    }

    private fun shouldHandlePlaybackPreparerAction(action: Long): Boolean {
        return (connector.player != null && (
                connector.playbackPreparer != null &&
                        connector.playbackPreparer!!.getSupportedPrepareActions() and action != 0L ||
                        connector.shouldDispatchUnsupportedActions)
                )
    }

    private fun shouldHandlePlaybackAction(action: Long): Boolean {
        return (connector.player != null && (
                connector.enabledPlaybackActions and action != 0L ||
                        connector.shouldDispatchUnsupportedActions)
                )
    }

    private fun shouldHandleQueueNavigatorAction(action: Long): Boolean {
        return (connector.player != null && (
                    connector.queueNavigator != null &&
                    connector.queueNavigator!!.getSupportedQueueNavigatorActions(connector.player!!) and action != 0L ||
                    connector.shouldDispatchUnsupportedActions)
                )
    }

    private fun shouldHandleRatingAction(): Boolean {
        return connector.player != null && connector.ratingCallback != null
    }

    private fun shouldHandleQueueEditAction(): Boolean {
        return connector.player != null && connector.queueEditor != null
    }
}
