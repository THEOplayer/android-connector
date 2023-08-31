package com.theoplayer.android.connector.analytics.conviva

import android.content.Context
import com.theoplayer.android.api.player.Player

typealias ConvivaMetadata = Map<String, Any>

class ConvivaConnector(
    appContext: Context,
    player: Player,
    convivaMetadata: ConvivaMetadata,
    convivaConfig: ConvivaConfiguration
) {

    private val convivaHandler: ConvivaHandler

    init {
        convivaHandler = ConvivaHandler(appContext, player, convivaMetadata, convivaConfig)
    }

    /**
     * Sets Conviva metadata on the Conviva video analytics.
     * @param metadata object of key value pairs
     */
    fun setContentInfo(metadata: ConvivaMetadata) {
        this.convivaHandler.setContentInfo(metadata)
    }

    /**
     * Sets Conviva metadata on the Conviva ad analytics.
     * @param metadata object of key value pairs
     */
    fun setAdInfo(metadata: ConvivaMetadata) {
        this.convivaHandler.setAdInfo(metadata)
    }

    /**
     * Explicitly stop the current session and start a new one.
     *
     * This can be used to manually mark the start of a new session during a live stream,
     * for example when a new program starts.
     * By default, new sessions are only started on play-out of a new source, or when an ad break starts.
     *
     * @param metadata object of key value pairs.
     */
    fun stopAndStartNewSession(metadata: ConvivaMetadata) {
        this.convivaHandler.stopAndStartNewSession(metadata)
    }

    /**
     * Sets an error to the conviva session and closes the session.
     * @param errorMessage string explaining what the error is.
     */
    fun reportPlaybackFailed(errorMessage: String) {
        this.convivaHandler.reportPlaybackFailed(errorMessage)
    }

    /**
     * Stops video and ad analytics and closes all sessions.
     */
    fun destroy() {
        this.convivaHandler.destroy()
    }
}