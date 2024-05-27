package com.theoplayer.android.connector.yospace

interface YospaceListener {
    /**
     * Fired when a new Yospace session starts.
     */
    fun onSessionAvailable() {}
}