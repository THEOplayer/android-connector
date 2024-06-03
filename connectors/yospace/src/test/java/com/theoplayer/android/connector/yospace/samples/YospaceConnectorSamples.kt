package com.theoplayer.android.connector.yospace.samples

import android.app.Activity
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.connector.yospace.YospaceConnector
import com.theoplayer.android.connector.yospace.YospaceSsaiDescription
import com.theoplayer.android.connector.yospace.YospaceStreamType

fun createYospaceConnector(activity: Activity) {
    // Set up the player and the connector
    val theoplayerView = THEOplayerView(activity)
    val yospaceConnector = YospaceConnector(theoplayerView)

    // Play a stream with Yospace SSAI
    theoplayerView.player.source = SourceDescription.Builder(
        TypedSource.Builder("https://example.com/stream.m3u8")
            .ssai(
                YospaceSsaiDescription(streamType = YospaceStreamType.LIVE)
            )
            .build()
    ).build()
}