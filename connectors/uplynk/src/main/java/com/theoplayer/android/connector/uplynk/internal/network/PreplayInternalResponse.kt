package com.theoplayer.android.connector.uplynk.internal.network

import com.theoplayer.android.connector.uplynk.network.DrmResponse
import com.theoplayer.android.connector.uplynk.network.PreplayResponse
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

    /*
    * The content protection information. (**Nullable**)
    */
    val drm: DrmResponse?
)


internal class PreplayInternalResponse(val body: String, private val json: Json) {
    fun parseMinimalResponse(): MinimalPreplayResponse = json.decodeFromString(body)
    fun parseExternalResponse(): PreplayResponse = json.decodeFromString(body)
}