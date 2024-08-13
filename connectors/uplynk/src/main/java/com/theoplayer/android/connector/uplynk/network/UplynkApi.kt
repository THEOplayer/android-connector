package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.json.Json

internal class UplynkApi {
    private val json = Json{ ignoreUnknownKeys = true }
    private val network = HttpsConnection()

    suspend fun preplay(srcURL: String): PreplayResponse {
        val body = network.get(srcURL)
        return json.decodeFromString(body)
    }
}
