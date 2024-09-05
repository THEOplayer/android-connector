package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

/**
 * The Uplynk Preplay Response API.
 *
 * For further details, please refer to the Uplynk Documentation:
 * [Preplay API (Version 2) Documentation](https://docs.edgecast.com/video/#Develop/Preplayv2.htm)
 */
@Serializable
data class PreplayResponse(

    /**
     * The manifest's URL. (**NonNull**)
     */
    val playURL: String,

    /**
     * The identifier of the viewer's session. (**NonNull**)
     */
    val sid: String,

    /*
    * The content protection information. (**Nullable**)
    */
    val drm: DrmResponse?,

    /**
     * The zone prefix for the viewer's session. (**NonNull**)
     *  * Use this prefix when submitting playback or API requests for this session.
     *
     * Example:
     *  * Possible return value: 'https://content-ause2.uplynk.com/'
     */
    val prefix: String,

    /**
     * Contains ad information, such as break offsets and non-video ads. (**NonNull**)
     *
     */
    val ads: UplynkAds,

    /**
     * Indicates the URL to the XML file containing interstitial information for Apple TV.
     * This parameter reports null when ads are not found.
     */
    val interstitialURL: String? = null)
