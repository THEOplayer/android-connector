package com.theoplayer.android.connector.uplynk.internal.events

import com.theoplayer.android.connector.uplynk.events.UplynkAssetInfoResponseErrorEvent
import com.theoplayer.android.connector.uplynk.events.UplynkEventTypes
import java.util.Date

internal class UplynkAssetInfoErrorResponseEventImpl(
    date: Date,
    private val body: String,
    private val exception: Exception?
) :
    UplynkEventImpl<UplynkAssetInfoResponseErrorEvent>(
        UplynkEventTypes.ASSET_INFO_RESPONSE_ERROR,
        date
    ),
    UplynkAssetInfoResponseErrorEvent {
    override fun getException() = exception

    override fun getBody() = body
}