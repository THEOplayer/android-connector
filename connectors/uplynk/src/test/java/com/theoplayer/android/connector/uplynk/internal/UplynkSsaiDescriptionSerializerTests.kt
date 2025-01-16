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
    fun givenEmptySsaiDescription_WhenSerialize_ThenReturnsExpected() {
        val ssaiDescription = UplynkSsaiDescription()
        val jsonString = UplynkSsaiDeserializer.toJson(ssaiDescription)
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        assertEquals(jsonObject.keys, setOf("integration"))
        assertEquals(jsonObject["integration"], JsonPrimitive(UplynkConnector.INTEGRATION_ID))
    }

    @Test
    fun givenSsaiDescriptionWithStreamType_WhenSerialize_ThenReturnsExpected() {
        val ssaiDescription = UplynkSsaiDescription(
            prefix = "preplayprefix",
            assetIds = listOf("asset1", "asset2", "asset3"),
            preplayParameters = LinkedHashMap(mapOf("p1" to "v1", "p2" to "v2", "p3" to "v3"))
        )
        val jsonString = UplynkSsaiDeserializer.toJson(ssaiDescription)
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
}
