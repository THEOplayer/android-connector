package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.common.EventDispatcherImpl
import com.theoplayer.android.connector.uplynk.internal.events.UplynkAssetInfoResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.events.UplynkPreplayResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.network.PreplayInternalResponse
import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.Date

internal class UplynkEventDispatcher(val eventDispatcher: EventDispatcherImpl) {

    fun dispatchPreplayEvents(response: PreplayInternalResponse) {
        val preplayResponse = response.parseExternalResponse()
        eventDispatcher.dispatchEvent(
            UplynkPreplayResponseEventImpl(
                Date(),
                preplayResponse
            )
        )
    }

    fun dispatchAssetInfoEvents(assetInfo: AssetInfoResponse) =
        CoroutineScope(Dispatchers.IO).async {
            eventDispatcher.dispatchEvent(
                UplynkAssetInfoResponseEventImpl(
                    Date(),
                    assetInfo
                )
            )
        }
}
