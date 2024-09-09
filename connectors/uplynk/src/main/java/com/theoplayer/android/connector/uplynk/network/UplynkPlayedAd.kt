package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A class that represents an advertisement that was played in live channel before playback started on the client
 */
@Serializable
data class UplynkPlayedAd (
    /**
     * Indicates the duration, in seconds, of an ad break.
     */
    val duration: Double,

    /**
     * Indicates the duration, in seconds, of an ad break.
     */
    val ts: Double,

    /**
     * **VAST Only**
     *
     * Contains the custom set of VAST extensions returned by the ad server.
     *
     * This is returned as a JsonElement because the extensions structure is custom.
     * You could build deserialization logic if needed depending on the expected structure of this field
     *
     * Check more info in [documentation](https://docs.edgecast.com/video/#AdIntegration/VAST-VPAID.htm#CustomVASTExt)
     */
    val extensions: JsonElement? = null,

    /**
     * **FreeWheel Only**
     * If the ad response provided by FreeWheel contains creative parameters,
     * they will be reported as name-value pairs within this object.
     *
     * Check more info in [documentation](https://docs.edgecast.com/video/#AdIntegration/Freewheel.htm)
     */
    @SerialName("fw_parameters")
    val freeWheelParameters: Map<String, String>
)
