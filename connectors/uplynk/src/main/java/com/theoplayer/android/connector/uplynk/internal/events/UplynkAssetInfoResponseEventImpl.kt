package com.theoplayer.android.connector.uplynk.internal.events

import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import java.util.Date

class UplynkAssetInfoResponseEventImpl(
    date: Date,
    private val response: AssetInfoResponse
) :
    UplynkEventImpl<UplynkAssetInfoResponseEvent>(UplynkEventTypes.ASSET_INFO_RESPONSE, date),
    UplynkAssetInfoResponseEvent {
        override fun getResponse(): AssetInfoResponse = response
    }