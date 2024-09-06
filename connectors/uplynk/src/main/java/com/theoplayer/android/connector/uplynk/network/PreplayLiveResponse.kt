package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

/**
 * The Uplynk Preplay Response for live channels and events.
 *
 * For further details, please refer to the Uplynk Documentation:
 * [Preplay API (Version 2) Documentation](https://docs.edgecast.com/video/#Develop/Preplayv2.htm)
 */
@Serializable
data class PreplayLiveResponse(

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
     *
     *
     *  * Use this prefix when submitting playback or API requests for this session.
     *
     *
     *
     * Example:
     *
     *  * Possible return value: 'https://content-ause2.uplynk.com/'
     *
     */
    val prefix: String,

    /**
     * Contains a list of ads that took place during the time period defined by the ts and endts request parameters.
     *
     */
    val ads: List<UplynkPlayedAd> = listOf()
)
