package com.theoplayer.android.connector.uplynk.internal.network

import com.theoplayer.android.connector.uplynk.network.DrmResponse
import com.theoplayer.android.connector.uplynk.network.PreplayLiveResponse
import com.theoplayer.android.connector.uplynk.network.PreplayVodResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
internal data class MinimalPreplayResponse(

    /**
     * The manifest's URL. (**NonNull**)
     */
    val playURL: String,

    /**
     * The identifier of the viewer's session. (**NonNull**)
     */
    val sid: String,

    /**
     * The zone prefix for the viewer's session. (**NonNull**)
     */
    val prefix: String,

    /*
    * The content protection information. (**Nullable**)
    */
    val drm: DrmResponse?
)


internal class PreplayInternalResponse(val body: String, private val json: Json) {
    fun parseMinimalResponse(): MinimalPreplayResponse = json.decodeFromString(body)
    fun parseExternalResponse(): PreplayVodResponse = json.decodeFromString(body)
}

internal class PreplayInternalLiveResponse(val body: String, private val json: Json) {
    fun parseMinimalResponse(): MinimalPreplayResponse = json.decodeFromString(body)
    fun parseExternalResponse(): PreplayLiveResponse = json.decodeFromString(body)
}