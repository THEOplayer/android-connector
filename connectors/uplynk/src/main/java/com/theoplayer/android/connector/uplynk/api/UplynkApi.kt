package com.theoplayer.android.connector.uplynk.api

import android.util.Log
import kotlinx.serialization.json.Json

class UplynkApi() {
    private val json = Json{ ignoreUnknownKeys = true }
    private val network = HttpsConnection()
    suspend fun preplay(srcURL: String): PreplayResponse {
        val body = network.get(srcURL)
        Log.d("OlegSPY", "response: $body")
        return try {
            json.decodeFromString(body)
        } catch (e: Exception) {
            Log.d("OlegSPY", "ex ", e)
            throw e
        }
    }

}
