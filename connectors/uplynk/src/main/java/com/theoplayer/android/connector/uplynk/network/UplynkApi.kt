package com.theoplayer.android.connector.uplynk.network

import com.theoplayer.android.connector.uplynk.common.HttpsConnection
import com.theoplayer.android.connector.uplynk.internal.network.PreplayInternalResponse
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
}

internal class UplynkApi {
    private val network = HttpsConnection()

    suspend fun preplay(srcURL: String): PreplayInternalResponse {
        val body = network.get(srcURL)
        return PreplayInternalResponse(body)
    }

    suspend fun assetInfo(url: String): AssetInfoResponse {
        val body = network.get(url)
        return json.decodeFromString(body)
    }
}