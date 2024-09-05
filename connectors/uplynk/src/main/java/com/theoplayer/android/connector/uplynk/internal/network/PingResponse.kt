package com.theoplayer.android.connector.uplynk.internal.network

import com.theoplayer.android.connector.uplynk.network.BoundaryDetail
import com.theoplayer.android.connector.uplynk.network.UplynkAds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration

/**
 * The response for a ping request
 * See more in [documentation](https://docs.edgecast.com/video/#Develop/Pingv2.htm)
 */
@Serializable
data class PingResponse(
    /**
     * Indicates the next playback position, in seconds, at which the player should request this endpoint.
     * The player should not issue additional API requests when this parameter returns -1.0.
     */
    @SerialName("next_time")
    @Serializable(with = DurationToSecDeserializer::class)
    val nextTime: Duration,

    /**
     * Contains information about upcoming ads.
     */
    val ads: UplynkAds? = null,

    /**
     * **VAST Only**
     *
     * Contains the custom set of VAST extensions returned by the ad server.
     * Each custom extension is reported as an object
     *
     * This is returned as a JsonElement because the extensions structure is custom.
     * You could build deserialisation logic if needed depending on the expected structure of this field
     *
     * Check more info in [documentation](https://docs.edgecast.com/video/#AdIntegration/VAST-VPAID.htm#CustomVASTExt)
     */
    val extensions: JsonElement? = null,

    /**
     * **Error Response Only**
     *
     * Describes the error that occurred.
     */
    val error: String? = null
)
