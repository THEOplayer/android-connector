package com.theoplayer.android.connector.yospace

import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionSerializer
import com.theoplayer.android.connector.yospace.internal.SerializedSessionProperties
import com.theoplayer.android.connector.yospace.internal.deserialize
import com.theoplayer.android.connector.yospace.internal.serialize
import com.yospace.admanagement.Session
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

/**
 * The configuration for server-side ad insertion using the Yospace connector.
 */
class YospaceSsaiDescription(
    /**
     * The type of the requested stream.
     *
     * Default: [YospaceStreamType.LIVE]
     */
    val streamType: YospaceStreamType = YospaceStreamType.LIVE,
    /**
     * Custom properties to set when initializing the Yospace session.
     */
    val sessionProperties: Session.SessionProperties = Session.SessionProperties()
) : CustomSsaiDescription() {
    override val customIntegrationId: String
        get() = INTEGRATION_ID
}

/**
 * The type of the Yospace stream.
 */
@Serializable
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
        return Json.decodeFromString(YospaceSsaiDescriptionKSerializer(), json)
    }

    override fun toJson(value: CustomSsaiDescription): String {
        return Json.encodeToString(YospaceSsaiDescriptionKSerializer(), value as YospaceSsaiDescription)
    }
}

@Serializable
private data class SerializedYospaceSsaiDescription(
    val integration: String,
    val streamType: YospaceStreamType,
    val sessionProperties: SerializedSessionProperties
)

private fun YospaceSsaiDescription.serialize() = SerializedYospaceSsaiDescription(
    integration = customIntegrationId,
    streamType = streamType,
    sessionProperties = sessionProperties.serialize()
)

private fun SerializedYospaceSsaiDescription.deserialize() = YospaceSsaiDescription(
    streamType = streamType,
    sessionProperties = sessionProperties.deserialize()
)

private class YospaceSsaiDescriptionKSerializer : KSerializer<YospaceSsaiDescription> {
    private val delegateSerializer = SerializedYospaceSsaiDescription.serializer()

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("YospaceSsaiDescription", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: YospaceSsaiDescription) {
        encoder.encodeSerializableValue(delegateSerializer, value.serialize())
    }

    override fun deserialize(decoder: Decoder): YospaceSsaiDescription {
        return decoder.decodeSerializableValue(delegateSerializer).deserialize()
    }
}
