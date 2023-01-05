package com.theoplayer.android.connector.mediasession

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import com.theoplayer.android.api.player.Player

/**
 * CustomActionProvider allows handling custom actions when sent by a media controller.
 */
interface CustomActionProvider {

    /**
     * Called when a media controller wants a custom action to be performed.
     *
     * @param player The THEOplayer instance currently attached to the connector.
     * @param action The action that was originally sent in the [PlaybackStateCompat.CustomAction].
     * @param extras Optional extras specified by the [android.support.v4.media.session.MediaControllerCompat].
     */
    fun onCustomAction(player: Player, action: String, extras: Bundle?)

    /**
     * Get the custom action this provider can handle.
     *
     * @param The THEOplayer instance currently attached to the connector.
     * @return A [PlaybackStateCompat.CustomAction] instance.
     */
    fun getCustomAction(player: Player): PlaybackStateCompat.CustomAction?
}
