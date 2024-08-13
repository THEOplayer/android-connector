package com.theoplayer.android.connector.uplynk.internal.events

import com.theoplayer.android.connector.uplynk.events.UplynkAssetInfoResponseEvent
import com.theoplayer.android.connector.uplynk.events.UplynkEventTypes
import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import java.util.Date

internal class UplynkAssetInfoResponseEventImpl(
    date: Date,
    override val response: AssetInfoResponse
) :
    UplynkEventImpl<UplynkAssetInfoResponseEvent>(UplynkEventTypes.ASSET_INFO_RESPONSE, date),
    UplynkAssetInfoResponseEvent