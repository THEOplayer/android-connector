package com.theoplayer.android.connector.yospace.internal

import com.theoplayer.android.connector.yospace.YospaceConnector
import com.theoplayer.android.connector.yospace.YospaceSsaiDescription
import com.theoplayer.android.connector.yospace.YospaceSsaiDescriptionSerializer
import com.theoplayer.android.connector.yospace.YospaceStreamType
import com.yospace.admanagement.Session.SessionProperties
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class YospaceSsaiDescriptionSerializerTests {
    @Test
    fun `given empty SSAI description, when serialize, then returns expected object`() {
        val ssaiDescription = YospaceSsaiDescription()
        val jsonString = YospaceSsaiDescriptionSerializer.toJson(ssaiDescription)
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        assertEquals(jsonObject["integration"], JsonPrimitive(YospaceConnector.INTEGRATION_ID))
        assertEquals(jsonObject["streamType"], JsonPrimitive(YospaceStreamType.LIVE.toString()))
        val jsonSessionProperties = jsonObject["sessionProperties"]?.jsonObject
        assertEquals(
            jsonSessionProperties?.keys, setOf(
                "requestTimeout",
                "resourceTimeout",
                "userAgent",
                "proxyUserAgent",
                "keepProxyAlive",
                "prefetchResources",
                "fireHistoricalBeacons",
                "applyEncryptedTracking",
                "excludedCategories",
                "consecutiveBreakTolerance",
                "token",
                "customHttpHeaders"
            )
        )
    }

    @Test
    fun `given SSAI description with stream type, when serialize, then returns expected object`() {
        val ssaiDescription = YospaceSsaiDescription(
            streamType = YospaceStreamType.LIVEPAUSE
        )
        val jsonString = YospaceSsaiDescriptionSerializer.toJson(ssaiDescription)
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        assertEquals(jsonObject["streamType"], JsonPrimitive(YospaceStreamType.LIVEPAUSE.toString()))
    }

    @Test
    fun `given SSAI description with session properties, when serialize, then returns expected object`() {
        val testUserAgent = "Test User Agent"
        val testHeaders = mapOf("X-Hello" to "World")
        val ssaiDescription = YospaceSsaiDescription(
            sessionProperties = SessionProperties().apply {
                userAgent = testUserAgent
                customHttpHeaders = testHeaders
            }
        )
        val jsonString = YospaceSsaiDescriptionSerializer.toJson(ssaiDescription)
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        val jsonSessionProperties = jsonObject["sessionProperties"]?.jsonObject
        assertEquals(jsonSessionProperties?.get("userAgent"), JsonPrimitive(testUserAgent))
        assertEquals(jsonSessionProperties?.get("customHttpHeaders"), JsonObject(testHeaders.mapValues { JsonPrimitive(it.value) }))
    }

    @Test
    fun `given SSAI description with session properties, when serialize and deserialize, then returns equivalent object`() {
        val testUserAgent = "Test User Agent"
        val testHeaders = mapOf("X-Hello" to "World")
        val ssaiDescription = YospaceSsaiDescription(
            sessionProperties = SessionProperties().apply {
                userAgent = testUserAgent
                customHttpHeaders = testHeaders
            }
        )
        val jsonString = YospaceSsaiDescriptionSerializer.toJson(ssaiDescription)
        val deserializedSsaiDescription = YospaceSsaiDescriptionSerializer.fromJson(jsonString)
        assertEquals(ssaiDescription.streamType, deserializedSsaiDescription.streamType)
        assertEquals(
            ssaiDescription.sessionProperties.serialize(),
            deserializedSsaiDescription.sessionProperties.serialize()
        )
    }
}
