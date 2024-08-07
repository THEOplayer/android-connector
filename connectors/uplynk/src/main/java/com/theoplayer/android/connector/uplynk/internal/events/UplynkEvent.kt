package com.theoplayer.android.connector.uplynk.internal.events

import com.theoplayer.android.api.event.Event

/**
 * The base Uplynk Event.
 */
interface UplynkEvent<E : UplynkEvent<E>> : Event<E>

interface UplynkPreplayResponseEvent: UplynkEvent<UplynkPreplayResponseEvent>

interface UplynkAssetInfoResponseEvent: UplynkEvent<UplynkAssetInfoResponseEvent>