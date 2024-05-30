package com.theoplayer.android.connector.yospace

import android.content.Context
import com.yospace.admanagement.LinearCreative

fun interface YospaceClickThroughCallback {
    fun onClickThrough(context: Context)
}

interface YospaceUiHandler {
    fun showLinearClickThrough(creative: LinearCreative, callback: YospaceClickThroughCallback)

    fun hideLinearClickThrough()

    fun destroy()
}