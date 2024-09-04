package com.theoplayer.android.connector.uplynk.internal.network

import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import kotlinx.serialization.json.Json


internal class UplynkApi {
    private val json = Json { ignoreUnknownKeys = true }
    private val network = HttpsConnection()

    suspend fun preplay(srcURL: String): PreplayInternalResponse {
        val body = network.retry { get(srcURL) }
        return PreplayInternalResponse(body, json)
    }

    suspend fun assetInfo(url: String): AssetInfoResponse {
        val body = network.retry { get(url) }
        return json.decodeFromString(body)
    }
}
