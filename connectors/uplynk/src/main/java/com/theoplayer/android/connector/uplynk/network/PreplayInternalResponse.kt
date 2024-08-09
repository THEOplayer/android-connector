package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
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
    val internalResponse: MinimalPreplayResponse = json.decodeFromString(body)
    var error: Exception? = null
    val externalResponse: PreplayResponse? by lazy {
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