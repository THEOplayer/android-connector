package com.theoplayer.android.connector.uplynk.network

import com.theoplayer.android.connector.uplynk.internal.network.DurationToSecDeserializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration


/**
 * Represents details about an ad, including its API framework, companion ads, creative details, and other properties.
 *
 * @property apiFramework Indicates the API Framework for the ad (e.g., VPAID). A null value is returned for ads that do not have an API Framework.
 * @property companions List of companion ads that go with the ad. Companion ads are also ad objects.
 * @property mimeType Indicates the ad's Internet media type (aka mime-type).
 * @property creative If applicable, indicates the creative to display. For video ads, this is the asset ID from the CMS. For VPAID ads, this is the URL to the VPAID JS or SWF.
 * @property events Object containing all of the events for this ad. Each event type contains an array of URLs.
 * @property width If applicable, indicates the width of the creative. This parameter reports "0" for the width/height of video ads.
 * @property height If applicable, indicates the height of the creative.
 * @property duration Indicates the duration, in seconds, of an ad's encoded video. For VPAID ads, this parameter reports the duration returned from the ad server.
 * @property extensions Contains the custom set of VAST extensions returned by the ad server. Each custom extension is reported as an object.
 * @property fwParameters FreeWheel only: If the ad response provided by FreeWheel contains creative parameters, they are reported as name-value pairs within this object.
 */
@Serializable
data class UplynkAd(
    /**
     * Indicates the API Framework for the ad (e.g., VPAID).
     * A null value is returned for ads that do not have an API Framework.
     */
    val apiFramework: String?,

    /**
     * List of companion ads that go with the ad.
     * Companion ads are also ad objects.
     */
    val companions: List<UplynkAd>,

    /**
     * Indicates the ad's Internet media type (aka mime-type).
     */
    val mimeType: String,

    /**
     * If applicable, indicates the creative to display.
     * Video Ad (CMS): Indicates the asset ID for the video ad pushed from the CMS.
     * Video Ad (VPAID): Indicates the URL to the VPAID JS or SWF.
     */
    val creative: String,

    /**
     * Object containing all of the events for this ad.
     * Each event type contains an array of URLs.
     */
    val events: Map<String, List<String>>? = null,

    /**
     * If applicable, indicates the width of the creative.
     * This parameter reports "0" for the width/height of video ads.
     */
    val width: Float,

    /**
     * If applicable, indicates the height of the creative.
     */
    val height: Float,

    /**
     * Indicates the duration, in seconds, of an ad's encoded video.
     * VPAID: For VPAID ads, this parameter reports the duration returned from the ad server.
     */
    @Serializable(with = DurationToSecDeserializer::class)
    val duration: Duration,

    /**
     * Contains the custom set of VAST extensions returned by the ad server.
     * Each custom extension is reported as an object.
     *
     * This is returned as a JsonElement because the extensions structure is custom.
     * You could build deserialisation logic if needed depending on the expected structure of this field
     *
     * Check more info in [documentation](https://docs.edgecast.com/video/#AdIntegration/VAST-VPAID.htm#CustomVASTExt)
     */
    val extensions: JsonElement? = null,

    /**
     * FreeWheel only: If the ad response provided by FreeWheel contains creative parameters,
     * they are reported as name-value pairs within this object.
     */
    val fwParameters: Map<String, String>? = null
)
