package com.theoplayer.android.connector.uplynk.internal.events

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
    fun getResponse(): PreplayResponse
}

/**
 * Represents an error response event for a Uplynk preplay request.
 * This event captures any exception that occurred and the response body.
 */
interface UplynkPreplayErrorResponseEvent : UplynkEvent<UplynkPreplayErrorResponseEvent> {
    /**
     * Retrieves the exception that occurred during the preplay request
     * or during parsing the response from preplay request.
     *
     * @return the exception
     */
    fun getException(): Exception?

    /**
     * Retrieves the response body from the preplay request.
     *
     * @return the response body as a string
     */
    fun getBody(): String
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
    fun getResponse(): AssetInfoResponse
}

/**
 * Represents an error response event for a Uplynk asset information request.
 * This event captures any exception that occurred and the raw response body.
 */
interface UplynkAssetInfoResponseErrorEvent : UplynkEvent<UplynkAssetInfoResponseErrorEvent> {
    /**
     * Retrieves the exception that occurred during the asset info request
     * or during parsing the asset info request
     *
     * @return the exception
     */
    fun getException(): Exception?

    /**
     * Retrieves the response body from the asset information request.
     *
     * @return the response body as a string
     */
    fun getBody(): String
}
