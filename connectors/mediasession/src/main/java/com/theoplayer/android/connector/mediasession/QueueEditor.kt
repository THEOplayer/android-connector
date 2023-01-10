package com.theoplayer.android.connector.mediasession

import android.support.v4.media.MediaDescriptionCompat
import com.theoplayer.android.api.player.Player

/**
 * QueueEditor allows handling queue editing actions when sent by a media controller.
 */
interface QueueEditor {
    /**
     * Called when a media controller wants to add a queue item with the given description at the
     * end of the play queue.
     *
     * @param player The THEOplayer instance currently attached to the connector.
     * @param description Metadata for a media item suitable for display.
     */
    fun onAddQueueItem(player: Player, description: MediaDescriptionCompat)

    /**
     * Called when a media controller wants to add a queue item with the given description
     * at the specified position in the play queue
     *
     * @param player The THEOplayer instance currently attached to the connector.
     * @param description Metadata for a media item suitable for display.
     * @param index The index at which the created queue item is to be inserted.
     */
    fun onAddQueueItem(player: Player, description: MediaDescriptionCompat, index: Int)

    /**
     * Called when a media controller wants to remove the first occurrence of the specified
     * queue item with the given description in the play queue.
     *
     * @param player The THEOplayer instance currently attached to the connector.
     * @param description Metadata for a media item suitable for display.
     */
    fun onRemoveQueueItem(player: Player, description: MediaDescriptionCompat)
}
