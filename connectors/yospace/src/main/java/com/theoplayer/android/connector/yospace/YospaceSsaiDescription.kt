package com.theoplayer.android.connector.yospace

import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionSerializer
import com.theoplayer.android.connector.yospace.internal.YospaceSessionPropertiesKSerializer
import com.yospace.admanagement.Session
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The configuration for server-side ad insertion using the [YospaceConnector].
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class YospaceSsaiDescription @JvmOverloads constructor(
    /**
     * The type of the requested stream.
     *
     * Default: [YospaceStreamType.LIVE]
     */
    @EncodeDefault
    val streamType: YospaceStreamType = YospaceStreamType.LIVE,
    /**
     * Custom properties to set when initializing the Yospace session.
     */
    @EncodeDefault
    @Serializable(with = YospaceSessionPropertiesKSerializer::class)
    val sessionProperties: Session.SessionProperties = Session.SessionProperties()
) : CustomSsaiDescription() {
    override val customIntegration: String
        get() = YospaceConnector.INTEGRATION_ID

    /**
     * A builder for a [YospaceSsaiDescription].
     */
    class Builder() {
        private var streamType: YospaceStreamType = YospaceStreamType.LIVE
        private var sessionProperties: Session.SessionProperties = Session.SessionProperties()

        /**
         * Sets the type of the requested stream.
         */
        fun streamType(streamType: YospaceStreamType) = apply { this.streamType = streamType }

        /**
         * Sets the custom properties to set when initializing the Yospace session.
         */
        fun sessionProperties(sessionProperties: Session.SessionProperties) = apply { this.sessionProperties = sessionProperties }

        /**
         * Builds the [YospaceSsaiDescription].
         */
        fun build() = YospaceSsaiDescription(streamType = streamType, sessionProperties = sessionProperties)
    }
}

/**
 * The type of the Yospace stream.
 */
enum class YospaceStreamType {
    /**
     * The stream is a live stream.
     */
    LIVE,

    /**
     * The stream is a live stream with a large DVR window.
     */
    LIVEPAUSE,

    /**
     * The stream is a Non-Linear Start-Over stream.
     */
    NONLINEAR,

    /**
     * The stream is a video-on-demand stream.
     */
    VOD,
}

internal class YospaceSsaiDescriptionSerializer : CustomSsaiDescriptionSerializer {
    override fun fromJson(json: String): YospaceSsaiDescription {
        return Json.decodeFromString(YospaceSsaiDescriptionKSerializer, json)
    }

    override fun toJson(value: CustomSsaiDescription): String {
        return Json.encodeToString(
            YospaceSsaiDescriptionKSerializer,
            value as YospaceSsaiDescription
        )
    }
}

private object YospaceSsaiDescriptionKSerializer :
    JsonTransformingSerializer<YospaceSsaiDescription>(YospaceSsaiDescription.serializer()) {

    override fun transformSerialize(element: JsonElement): JsonElement = JsonObject(
        // Add integration to JSON
        buildMap {
            put("integration", JsonPrimitive(YospaceConnector.INTEGRATION_ID))
            putAll(element.jsonObject)
        }
    )

    override fun transformDeserialize(element: JsonElement): JsonElement {
        // Validate integration in JSON
        val integration = element.jsonObject["integration"]?.jsonPrimitive
        require(integration == JsonPrimitive(YospaceConnector.INTEGRATION_ID))
        return element
    }

}
