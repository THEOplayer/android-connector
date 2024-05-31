package com.theoplayer.android.connector.yospace.samples

import android.app.Activity
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.connector.yospace.YospaceConnector

fun createYospaceConnector(activity: Activity) {
    val theoplayerView = THEOplayerView(activity)
    val yospaceConnector = YospaceConnector(theoplayerView)
}