package com.theoplayer.android.connector.mediasession

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat

/**
 * PlaybackPreparer allows handling media prepare actions when sent by a media controller.
 */
interface PlaybackPreparer {
    companion object {
        const val AVAILABLE_ACTIONS = (PlaybackStateCompat.ACTION_PREPARE
                or PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
                or PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH
                or PlaybackStateCompat.ACTION_PREPARE_FROM_URI
                or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                or PlaybackStateCompat.ACTION_PLAY_FROM_URI)
    }

    /**
     * Get the supported prepare actions.
     *
     * @return A subset of [AVAILABLE_ACTIONS], indicating which actions are supported.
     */
    fun getSupportedPrepareActions(): Long

    /**
     * Called when a media controller wants to prepare playback.
     *
     * @param autoPlay Start playback when preparation is done.
     */
    fun onPrepare(autoPlay: Boolean)

    /**
     *  Called when a media controller wants to prepare playback of a specific mediaId.
     *
     * @param mediaId The mediaId provided.
     * @param autoPlay Start playback when preparation is done.
     * @param extras The extras can include information about the media.
     */
    fun onPrepareFromMediaId(mediaId: String?, autoPlay: Boolean, extras: Bundle?)

    /**
     *  Called when a media controller wants to prepare playback for a specific query.
     *
     * @param query The associated query.
     * @param autoPlay Start playback when preparation is done.
     * @param extras The extras can include information about the media.
     */
    fun onPrepareFromSearch(query: String?, autoPlay: Boolean, extras: Bundle?)

    /**
     *  Called when a media controller wants to prepare playback of a specific uri.
     *
     * @param uri The media Uri.
     * @param autoPlay Start playback when preparation is done.
     * @param extras The extras can include information about the media.
     */
    fun onPrepareFromUri(uri: Uri?, autoPlay: Boolean, extras: Bundle?)
}
