package com.theoplayer.android.connector.uplynk.events

import com.theoplayer.android.api.event.Event
import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import com.theoplayer.android.connector.uplynk.network.PreplayResponse

/**
 * The base Uplynk Event.
 */
interface UplynkEvent<E : UplynkEvent<E>> : Event<E>

/**
 * Represents a response event for a Uplynk preplay request.
 * This event carries the response payload from the preplay request.
 */
interface UplynkPreplayResponseEvent : UplynkEvent<UplynkPreplayResponseEvent> {
    /**
     * Retrieves the preplay response.
     *
     * @return the response from the preplay request
     */
    val response: PreplayResponse
}

/**
 * Represents a response event for a Uplynk asset information request.
 * This event carries the response payload from the asset information request.
 */
interface UplynkAssetInfoResponseEvent : UplynkEvent<UplynkAssetInfoResponseEvent> {
    /**
     * Retrieves the asset information response.
     *
     * @return the response from the asset information request
     */
    val response: AssetInfoResponse
}