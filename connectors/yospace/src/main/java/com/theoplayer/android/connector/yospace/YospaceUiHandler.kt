package com.theoplayer.android.connector.yospace

import android.content.Context
import com.yospace.admanagement.LinearCreative
import com.yospace.admanagement.NonLinearCreative

fun interface YospaceClickThroughCallback {
    fun onClickThrough(context: Context)
}

interface YospaceUiHandler {
    fun showLinearClickThrough(creative: LinearCreative, callback: YospaceClickThroughCallback)

    fun hideLinearClickThrough()

    fun showNonLinear(nonLinearCreative: NonLinearCreative, callback: YospaceClickThroughCallback)

    fun hideNonLinear(nonLinearCreative: NonLinearCreative)

    fun destroy()
}