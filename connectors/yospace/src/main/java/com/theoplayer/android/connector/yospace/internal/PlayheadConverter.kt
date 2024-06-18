package com.theoplayer.android.connector.yospace.internal

internal interface PlayheadConverter {
    fun fromPlayhead(playhead: Long): Double

    fun toPlayhead(playerTime: Double): Long
}