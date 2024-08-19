package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

/**
 * Represents an event containing details about an ad break.
 *
 * @property ads A list of ad objects associated with this ad break.
 * @property type Indicates the ad break type. Valid values are: "linear", "nonlinear".
 * @property position Indicates the position of the ad break. Valid values are: "preroll", "midroll", "postroll", "pause", "overlay".
 * @property timeOffset Indicates the start time of the ad break in the player timeline.
 * @property duration Indicates the duration of the ad break.
 */
@Serializable
data class UplynkAdBreaks(
    /**
     * A list of ad objects associated with this ad break.
     */
    val ads: List<UplynkAd>,

    /**
     * Indicates the ad break type.
     * Valid values are: "linear", "nonlinear".
     */
    val type: String,

    /**
     * Indicates the position of the ad break.
     * Valid values are: "preroll", "midroll", "postroll", "pause", "overlay".
     */
    val position: String,

    /**
     * Indicates the start time of the ad break in the player timeline.
     */
    val timeOffset: Float,

    /**
     * Indicates the duration of the ad break.
     */
    val duration: Float
)
