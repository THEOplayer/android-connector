package com.theoplayer.android.connector.uplynk

enum class SkippedAdStrategy {
    PLAY_NONE,
    PLAY_ALL,
    PLAY_LAST
}

data class UplynkConfiguration(
    val defaultSkipOffset: Int = -1,
    val onSeekOverAd: SkippedAdStrategy = SkippedAdStrategy.PLAY_NONE
)