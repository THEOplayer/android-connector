package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionSerializer
import com.theoplayer.android.connector.uplynk.UplynkConnector
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object UplynkSsaiDescriptionDeserializer : CustomSsaiDescriptionSerializer {
    private val json = Json { ignoreUnknownKeys = true }

    override fun fromJson(json: String): UplynkSsaiDescription {
        return this.json.decodeFromString(UplynkSsaiDescriptionKSerializer, json)
    }

    override fun toJson(value: CustomSsaiDescription): String {
        return this.json.encodeToString(
            UplynkSsaiDescriptionKSerializer,
            value as UplynkSsaiDescription
        )
    }
}

private object UplynkSsaiDescriptionKSerializer :
    JsonTransformingSerializer<UplynkSsaiDescription>(UplynkSsaiDescription.serializer()) {

    override fun transformSerialize(element: JsonElement): JsonElement = JsonObject(
        // Add integration to JSON
        buildMap {
            put("integration", JsonPrimitive(UplynkConnector.INTEGRATION_ID))
            putAll(element.jsonObject)
        }
    )

    override fun transformDeserialize(element: JsonElement): JsonElement {
        // Validate integration in JSON
        val integration = element.jsonObject["integration"]?.jsonPrimitive
        require(integration == JsonPrimitive(UplynkConnector.INTEGRATION_ID))
        return element
    }

}
