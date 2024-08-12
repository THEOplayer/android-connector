package com.theoplayer.android.connector.uplynk.internal.network

import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
}


internal class AssetInfoInternalResponse(val body: String) {
    var error: Exception? = null
    val externalResponse: AssetInfoResponse? by lazy {
        try {
            json.decodeFromString(body)
        } catch (se: SerializationException) {
            error = se
            null
        } catch (ie: IllegalArgumentException) {
            error = ie
            null
        }
    }
}