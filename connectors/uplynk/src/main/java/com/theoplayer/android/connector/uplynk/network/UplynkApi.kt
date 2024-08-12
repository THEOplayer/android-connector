package com.theoplayer.android.connector.uplynk.network

import com.theoplayer.android.connector.uplynk.internal.network.AssetInfoInternalResponse
import com.theoplayer.android.connector.uplynk.internal.network.HttpsConnection
import com.theoplayer.android.connector.uplynk.internal.network.PreplayInternalResponse

internal class UplynkApi {
    private val network = HttpsConnection()

    suspend fun preplay(srcURL: String): PreplayInternalResponse {
        val body = network.get(srcURL)
        return PreplayInternalResponse(body)
    }

    suspend fun assetInfo(url: String): AssetInfoInternalResponse {
        val body = network.get(url)
        return AssetInfoInternalResponse(body)
    }
}
