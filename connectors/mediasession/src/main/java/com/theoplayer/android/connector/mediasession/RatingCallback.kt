package com.theoplayer.android.connector.mediasession

import com.theoplayer.android.api.player.Player
import android.support.v4.media.RatingCompat
import android.os.Bundle

/**
 * RatingCallback allows handling rating actions when sent by a media controller.
 */
interface RatingCallback {

    /**
     * Called when the media controller wants to set a rating for the item currently playing.
     *
     * @param player The THEOplayer instance currently attached to the connector.
     * @param rating The rating being set.
     */
    fun onSetRating(player: Player, rating: RatingCompat)

    /**
     * Called when the media controller wants to set a rating for the item currently playing.
     *
     * @param player The THEOplayer instance currently attached to the connector.
     * @param rating The rating being set.
     * @param extras The extras can include information about the media item being rated.
     */
    fun onSetRating(player: Player, rating: RatingCompat, extras: Bundle?)
}
