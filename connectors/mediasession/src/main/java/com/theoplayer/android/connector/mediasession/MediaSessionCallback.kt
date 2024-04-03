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
        connector.player?.let { player ->
            if (shouldHandlePlaybackAction(ACTION_PLAY)) {
                player.play()

                // Make sure the session is currently active and ready to receive commands.
                connector.setActive(true)
            }
            connector.listeners.forEach { listener ->
                listener.onPlay()
            }
        }
    }

    override fun onPause() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onPause")
        }
        connector.player?.let { player ->
            if (shouldHandlePlaybackAction(ACTION_PAUSE)) {
                player.pause()
            }
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
        connector.player?.let { player ->
            if (shouldHandlePlaybackAction(ACTION_STOP)) {
                player.stop()
                connector.setActive(false)
            }
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
            skip(connector.skipForwardInterval)
        }
        connector.listeners.forEach { listener ->
            listener.onFastForward()
        }
    }

    override fun onRewind() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onRewind")
        }
        if (shouldHandlePlaybackAction(ACTION_REWIND)) {
            skip(-connector.skipBackwardsInterval)
        }
        connector.listeners.forEach { listener ->
            listener.onRewind()
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
        connector.player?.let { player ->
            if (shouldHandlePlaybackAction(ACTION_SET_PLAYBACK_SPEED)) {
                player.playbackRate = speed.toDouble()
            }
            connector.listeners.forEach { listener ->
                listener.onSetPlaybackSpeed(speed)
            }
        }
    }

    override fun onSkipToNext() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSkipToNext")
        }
        connector.player?.let { player ->
            if (shouldHandleQueueNavigatorAction(ACTION_SKIP_TO_NEXT)) {
                connector.queueNavigator?.onSkipToNext(player)
            }
            connector.listeners.forEach { listener ->
                listener.onSkipToNext()
            }
        }
    }

    override fun onSkipToPrevious() {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSkipToPrevious")
        }
        connector.player?.let { player ->
            if (shouldHandleQueueNavigatorAction(ACTION_SKIP_TO_PREVIOUS)) {
                connector.queueNavigator?.onSkipToPrevious(player)
            }
            connector.listeners.forEach { listener ->
                listener.onSkipToPrevious()
            }
        }
    }

    override fun onSkipToQueueItem(id: Long) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSkipToQueueItem")
        }
        connector.player?.let { player ->
            if (shouldHandleQueueNavigatorAction(ACTION_SKIP_TO_QUEUE_ITEM)) {
                connector.queueNavigator?.onSkipToQueueItem(player, id)
            }
            connector.listeners.forEach { listener ->
                listener.onSkipToQueueItem(id)
            }
        }
    }

    override fun onAddQueueItem(description: MediaDescriptionCompat) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onAddQueueItem")
        }
        connector.player?.let { player ->
            if (shouldHandleQueueEditAction()) {
                connector.queueEditor?.onAddQueueItem(player, description)
            }
            connector.listeners.forEach { listener ->
                listener.onAddQueueItem(description)
            }
        }
    }

    override fun onAddQueueItem(description: MediaDescriptionCompat, index: Int) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onAddQueueItem")
        }
        connector.player?.let { player ->
            if (shouldHandleQueueEditAction()) {
                connector.queueEditor?.onAddQueueItem(player, description, index)
            }
            connector.listeners.forEach { listener ->
                listener.onAddQueueItem(description)
            }
        }
    }

    override fun onRemoveQueueItem(description: MediaDescriptionCompat) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onRemoveQueueItem")
        }
        connector.player?.let { player ->
            if (shouldHandleQueueEditAction()) {
                connector.queueEditor?.onRemoveQueueItem(player, description)
            }
            connector.listeners.forEach { listener ->
                listener.onRemoveQueueItem(description)
            }
        }
    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onCustomAction $action")
        }
        connector.player?.let { player ->
            for (customActionProvider in connector.customActionProviders) {
                if (customActionProvider.getCustomAction(player)?.action == action) {
                    customActionProvider.onCustomAction(player, action, extras)
                }
            }
            connector.listeners.forEach { listener ->
                listener.onCustomAction(action, extras)
            }
        }
    }

    override fun onSeekTo(positionMs: Long) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSeekTo $positionMs")
        }
        connector.player?.let { player ->
            if (shouldHandlePlaybackAction(ACTION_SEEK_TO)) {
                player.currentTime = 1e-03 * positionMs
            }
            connector.listeners.forEach { listener ->
                listener.onSeekTo(positionMs)
            }
        }
    }

    override fun onSetRating(rating: RatingCompat) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSetRating $rating")
        }
        connector.player?.let { player ->
            if (shouldHandleRatingAction()) {
                connector.ratingCallback?.onSetRating(player, rating)
            }
            connector.listeners.forEach { listener ->
                listener.onSetRating(rating, null)
            }
        }
    }

    override fun onSetRating(rating: RatingCompat, extras: Bundle?) {
        if (connector.debug) {
            Log.d(TAG, "MediaSessionCallback::onSetRating $rating")
        }
        connector.player?.let { player ->
            if (shouldHandleRatingAction()) {
                connector.ratingCallback?.onSetRating(player, rating, extras)
            }
            connector.listeners.forEach { listener ->
                listener.onSetRating(rating, extras)
            }
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
