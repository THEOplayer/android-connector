package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

/**
 * An Uplynk ads data contains an information about ad breaks.
 *
 *
 * @property breaks A list of objects for every ad break in the ad response.
 * @property breakOffsets A list of objects that contain the ad break's timeOffset and the index for the ads.breaks object.
 * @property placeholderOffsets A list of objects with start and end times for every non-video ad that has been replaced with a short blank video (i.e., placeholder ad).
 */
@Serializable
data class UplynkAds(
    /**
     * A list of objects for every ad break in the ad response. This includes both linear and non-linear ads.
     */
    val breaks: List<UplynkAdBreak>,

    /**
     * A list of objects that contain the ad break's timeOffset and the index for the ads.breaks object.
     */
    val breakOffsets: List<UplynkBreakOffsets> = emptyList(),

    /**
     * A list of objects with start and end times for every non-video ad that has been replaced with a short blank video (i.e., placeholder ad).
     */
    val placeholderOffsets: List<UplynkPlaceholderAds> = emptyList()
)
