package com.theoplayer.android.connector.uplynk.internal.network

import com.theoplayer.android.connector.uplynk.network.PreplayResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json{
    ignoreUnknownKeys = true
}

@Serializable
internal data class MinimalPreplayResponse(

    /**
     * The manifest's URL. (**NonNull**)
     */
    val playURL: String,

    /**
     * The identifier of the viewer's session. (**NonNull**)
     */
    val sid: String)


internal class PreplayInternalResponse(val body: String) {
    fun parseMinimalResponse(): MinimalPreplayResponse = json.decodeFromString(body)
    fun parseExternalResponse(): PreplayResponse = json.decodeFromString(body)
}