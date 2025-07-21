package com.theoplayer.android.connector.analytics.gemius

import com.gemius.sdk.stream.Player

object Utils {
    fun describeEvent(eventType: Player.EventType): String = when (eventType) {
        Player.EventType.SKIP -> "SKIP"
        Player.EventType.PLAY -> "PLAY"
        Player.EventType.PAUSE -> "PAUSE"
        Player.EventType.STOP -> "STOP"
        Player.EventType.CLOSE -> "CLOSE"
        Player.EventType.BUFFER -> "BUFFER"
        Player.EventType.BREAK -> "BREAK"
        Player.EventType.SEEK -> "SEEK"
        Player.EventType.COMPLETE -> "COMPLETE"
        Player.EventType.NEXT -> "NEXT"
        Player.EventType.PREV -> "PREV"
        Player.EventType.CHANGE_VOL -> "CHANGE_VOL"
        Player.EventType.CHANGE_QUAL -> "CHANGE_QUAL"
        Player.EventType.CHANGE_RES -> "CHANGE_RES"
        else -> "UNKNOWN EVENTTYPE"
    }
}