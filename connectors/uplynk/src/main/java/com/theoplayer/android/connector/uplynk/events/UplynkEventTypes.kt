package com.theoplayer.android.connector.uplynk.events

import com.theoplayer.android.api.event.EventType
import com.theoplayer.android.connector.uplynk.internal.events.UplynkEventTypeImpl

object UplynkEventTypes {
    val PREPLAY_RESPONSE: EventType<UplynkPreplayResponseEvent> = UplynkEventTypeImpl("PREPLAY_RESPONSE")
    val PREPLAY_RESPONSE_ERROR: EventType<UplynkPreplayErrorResponseEvent> = UplynkEventTypeImpl("PREPLAY_RESPONSE_ERROR")
    val ASSET_INFO_RESPONSE: EventType<UplynkAssetInfoResponseEvent> = UplynkEventTypeImpl("ASSET_INFO_RESPONSE")
    val ASSET_INFO_RESPONSE_ERROR: EventType<UplynkAssetInfoResponseErrorEvent> = UplynkEventTypeImpl("ASSET_INFO_RESPONSE_ERROR")
}