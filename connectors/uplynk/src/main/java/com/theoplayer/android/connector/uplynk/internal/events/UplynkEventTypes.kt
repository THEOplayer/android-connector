package com.theoplayer.android.connector.uplynk.internal.events

object UplynkEventTypes {
    val PREPLAY_RESPONSE = UplynkEventTypeImpl<UplynkPreplayResponseEvent>("PREPLAY_RESPONSE")
    val PREPLAY_RESPONSE_ERROR = UplynkEventTypeImpl<UplynkPreplayErrorResponseEvent>("PREPLAY_RESPONSE_ERROR")
    val ASSET_INFO_RESPONSE = UplynkEventTypeImpl<UplynkAssetInfoResponseEvent>("ASSET_INFO_RESPONSE")
    val ASSET_INFO_RESPONSE_ERROR = UplynkEventTypeImpl<UplynkAssetInfoResponseErrorEvent>("ASSET_INFO_RESPONSE_ERROR")
}