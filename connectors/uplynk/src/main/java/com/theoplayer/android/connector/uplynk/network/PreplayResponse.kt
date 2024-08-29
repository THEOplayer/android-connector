package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

/**
 * The Uplynk Preplay Base Response API.
 *
 * For further details, please refer to the Uplynk Documentation:
 * [Preplay API (Version 2) Documentation](https://docs.edgecast.com/video/#Develop/Preplayv2.htm%3FTocPath%3DDevelop%7CClient%2520(Media%2520Player)%7C_____2)
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
     * Contains ad information, such as break offsets and non-video ads.
     *
     *  (**NonNull**)
     *
     */
    val ads: UplynkAds)
