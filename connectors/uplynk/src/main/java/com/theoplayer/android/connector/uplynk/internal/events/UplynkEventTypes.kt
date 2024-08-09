package com.theoplayer.android.connector.uplynk.internal.events

object UplynkEventTypes {
    val PREPLAY_RESPONSE = UplynkEventTypeImpl<UplynkPreplayResponseEvent>("PREPLAY_RESPONSE")
    val ASSET_INFO_RESPONSE = UplynkEventTypeImpl<UplynkAssetInfoResponseEvent>("ASSET_INFO_RESPONSE")
}