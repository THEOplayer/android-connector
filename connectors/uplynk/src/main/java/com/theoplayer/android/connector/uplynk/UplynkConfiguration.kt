package com.theoplayer.android.connector.uplynk

/**
 * Describes the configuration of the Uplynk Media integration.
 */
enum class SkippedAdStrategy {
    /**
     * Plays none of the ad breaks skipped due to a seek.
     */
    PLAY_NONE,

    /**
     * Plays all the ad breaks skipped due to a seek.
     */
    PLAY_ALL,

    /**
     * Plays the last ad break skipped due to a seek.
     */
    PLAY_LAST
}

/**
 * Describes the configuration of the Uplynk Media integration.
 */
data class UplynkConfiguration(
    /**
     * The offset after which an ad break may be skipped, in seconds.
     *
     * @remarks
     * If the offset is -1, the ad is unskippable.
     * If the offset is 0, the ad is immediately skippable.
     * Otherwise it must be a positive number indicating the offset.
     *
     * @defaultValue `-1`
     */
    val defaultSkipOffset: Int = -1,

    /**
     * The ad skip strategy which is used when seeking over ads.
     *
     * @defaultValue `PLAY_NONE`.
     */
    val onSeekOverAd: SkippedAdStrategy = SkippedAdStrategy.PLAY_NONE
)
