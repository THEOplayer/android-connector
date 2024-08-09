package com.theoplayer.android.connector.uplynk.internal.events

import com.theoplayer.android.api.event.Event
import com.theoplayer.android.connector.uplynk.network.PreplayResponse

/**
 * The base Uplynk Event.
 */
interface UplynkEvent<E : UplynkEvent<E>> : Event<E>

interface UplynkPreplayResponseEvent: UplynkEvent<UplynkPreplayResponseEvent> {
    fun getResponse(): PreplayResponse
}

interface UplynkAssetInfoResponseEvent: UplynkEvent<UplynkAssetInfoResponseEvent> {
    fun getResponse(): String
}