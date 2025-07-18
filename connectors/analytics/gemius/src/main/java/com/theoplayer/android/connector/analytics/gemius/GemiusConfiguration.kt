package com.theoplayer.android.connector.analytics.gemius

data class GemiusConfiguration (
    val applicationName: String,
    val applicationVersion: String,
    val hitCollectorHost: String,
    val gemiusId: String,
    val debug: Boolean,
    val adProcessor: AdProcessor?
)