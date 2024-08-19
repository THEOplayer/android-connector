package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

/**
 * Represents the details of a placeholder ad, including its start and end times, and the indices related to its position in the ad breaks array.
 *
 * @property startTime Indicates the starting time of the placeholder ad. This value is in player time for the entire m3u8 timeline.
 * @property endTime Indicates the ending time of the placeholder ad.
 * @property breaksIndex Indicates the index in the ads.breaks array that contains the VPAID ad that was replaced.
 * @property adsIndex Indicates the index in the ads.breaks.ads array that identifies the location for VPAID ad information.
 */
@Serializable
data class UplynkPlaceholderAds(
    /**
     * Indicates the starting time of the placeholder ad.
     * This value is in player time for the entire m3u8 timeline.
     */
    val startTime: Float,

    /**
     * Indicates the ending time of the placeholder ad.
     */
    val endTime: Float,

    /**
     * Indicates the index in the ads.breaks array that contains the VPAID ad that was replaced.
     */
    val breaksIndex: Int,

    /**
     * Indicates the index in the ads.breaks.ads array that identifies the location for VPAID ad information.
     */
    val adsIndex: Int
)
