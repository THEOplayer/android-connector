package com.theoplayer.android.connector.uplynk.internal.events

import java.util.Date

class UplynkAssetInfoResponseEventImpl(
    date: Date,
    private val response: String
) :
    UplynkEventImpl<UplynkAssetInfoResponseEvent>(UplynkEventTypes.ASSET_INFO_RESPONSE, date),
    UplynkAssetInfoResponseEvent {
        override fun getResponse(): String = response
    }