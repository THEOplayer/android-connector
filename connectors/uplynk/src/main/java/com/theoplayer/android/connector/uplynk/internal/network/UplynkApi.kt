package com.theoplayer.android.connector.uplynk.internal.network

import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import com.theoplayer.android.connector.uplynk.network.PingResponse
import kotlinx.serialization.json.Json


internal class UplynkApi {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val network = HttpsConnection()

    suspend fun preplayVod(srcURL: String): PreplayInternalVodResponse {
        val body = network.retry { get(srcURL) }
        return PreplayInternalVodResponse(body, json)
    }

    suspend fun preplayLive(srcURL: String): PreplayInternalLiveResponse {
        val body = network.retry { get(srcURL) }
        return PreplayInternalLiveResponse(body, json)
    }

    suspend fun assetInfo(url: String): AssetInfoResponse {
        val body = network.retry { get(url) }
        return json.decodeFromString(body)
    }

    suspend fun ping(url: String): PingResponse {
        val body = network.get(url)
        return json.decodeFromString(body)
    }
}
