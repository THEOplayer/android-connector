package com.theoplayer.android.connector.mediasession

import com.theoplayer.android.api.player.Player
import android.support.v4.media.session.PlaybackStateCompat

/**
 * QueueNavigator allows handling queue navigation actions when sent by a media controller.
 */
interface QueueNavigator {
    companion object {
        const val AVAILABLE_ACTIONS = (PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    }

    /**
     * Get supported queue navigator actions.
     *
     * @param player The THEOplayer instance currently attached to the connector.
     * @return A subset of [AVAILABLE_ACTIONS], indicating which actions are supported.
     */
    fun getSupportedQueueNavigatorActions(player: Player): Long

    /**
     * Get the active queue item id.
     *
     * @param player The THEOplayer instance currently attached to the connector.
     * @return The id of the currently active queue item.
     */
    fun getActiveQueueItemId(player: Player): Long

    /**
     * Called when the media controller indicates it wants skip to the previous media item.
     *
     * @param player The THEOplayer instance currently attached to the connector.
     */
    fun onSkipToPrevious(player: Player)

    /**
     * Called when the media controller indicates it wants skip to a specific item in queue.
     *
     * @param player The THEOplayer instance currently attached to the connector.
     * @param id The id of the queue item.
     */
    fun onSkipToQueueItem(player: Player, id: Long)

    /**
     * Called when the media controller indicates it wants skip to the next media item.
     *
     * @param player The THEOplayer instance currently attached to the connector.
     */
    fun onSkipToNext(player: Player)
}
