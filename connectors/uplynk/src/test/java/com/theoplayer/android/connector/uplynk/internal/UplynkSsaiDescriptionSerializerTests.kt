package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.UplynkConnector
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class UplynkSsaiDescriptionSerializerTests {
    @Test
    fun `given empty SSAI description, when serialize, then returns expected object`() {
        val ssaiDescription = UplynkSsaiDescription()
        val jsonString = UplynkSsaiDescriptionDeserializer.toJson(ssaiDescription)
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        assertEquals(jsonObject.keys, setOf("integration"))
        assertEquals(jsonObject["integration"], JsonPrimitive(UplynkConnector.INTEGRATION_ID))
    }

    @Test
    fun `given SSAI description with parameters, when serialize, then returns expected object`() {
        val ssaiDescription = UplynkSsaiDescription(
            prefix = "preplayprefix",
            assetIds = listOf("asset1", "asset2", "asset3"),
            preplayParameters = LinkedHashMap(mapOf("p1" to "v1", "p2" to "v2", "p3" to "v3"))
        )
        val jsonString = UplynkSsaiDescriptionDeserializer.toJson(ssaiDescription)
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        assertEquals(
            jsonObject.keys,
            setOf("integration", "prefix", "assetIds", "preplayParameters")
        )
        assertEquals(jsonObject["integration"], JsonPrimitive(UplynkConnector.INTEGRATION_ID))
        assertEquals(jsonObject["prefix"], JsonPrimitive(ssaiDescription.prefix))
        assertEquals(jsonObject["assetIds"]?.jsonArray?.size, 3)
        assertEquals(jsonObject["preplayParameters"]?.jsonObject?.size, 3)
    }

    @Test
    fun `given SSAI description with parameters, when serialize and deserialize, then returns equivalent object`() {
        val ssaiDescription = UplynkSsaiDescription(
            prefix = "preplayprefix",
            assetIds = listOf("asset1", "asset2", "asset3"),
            preplayParameters = LinkedHashMap(mapOf("p1" to "v1", "p2" to "v2", "p3" to "v3"))
        )
        val jsonString = UplynkSsaiDescriptionDeserializer.toJson(ssaiDescription)
        val deserializedSsaiDescription = UplynkSsaiDescriptionDeserializer.fromJson(jsonString)
        assertEquals(
            ssaiDescription,
            deserializedSsaiDescription
        )
    }
}
