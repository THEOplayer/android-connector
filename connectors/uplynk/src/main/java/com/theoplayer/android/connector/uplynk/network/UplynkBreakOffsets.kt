package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

/**
 * An object that contains the ad break's timeOffset and the index for the ads.breaks object.
 *
 * @property index The index of the ad break within the `ads.breaks` list.
 * @property timeOffset The time offset in seconds where the ad break occurs in the stream.
 */
@Serializable
data class UplynkBreakOffsets(
    /**
     * The index of the ad break within the `ads.breaks` list.
     */
    val index: Int,
    /**
     * The time offset in seconds where the ad break occurs in the stream.
     */
    val timeOffset: Double
)