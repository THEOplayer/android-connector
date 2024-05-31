package com.theoplayer.android.connector.yospace

/**
 * A listener that receives events from a [YospaceConnector].
 */
interface YospaceListener {
    /**
     * Fired when a new Yospace session starts.
     */
    fun onSessionAvailable() {}
}