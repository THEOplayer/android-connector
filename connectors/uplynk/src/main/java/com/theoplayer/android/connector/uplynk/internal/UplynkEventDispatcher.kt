package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.common.EventDispatcherImpl
import com.theoplayer.android.connector.uplynk.internal.events.UplynkAssetInfoErrorResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.events.UplynkAssetInfoResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.events.UplynkPreplayErrorResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.events.UplynkPreplayResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.network.AssetInfoInternalResponse
import com.theoplayer.android.connector.uplynk.internal.network.PreplayInternalResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.Date

internal class UplynkEventDispatcher(val eventDispatcher: EventDispatcherImpl) {

    fun dispatchPreplayEvents(response: PreplayInternalResponse) {
        if (response.externalResponse != null) {
            eventDispatcher.dispatchEvent(
                UplynkPreplayResponseEventImpl(
                    Date(),
                    response.externalResponse!!
                )
            )
        } else {
            eventDispatcher.dispatchEvent(
                UplynkPreplayErrorResponseEventImpl(
                    Date(),
                    response.body,
                    response.error
                )
            )
        }
    }

    fun dispatchAssetInfoEvents(assetInfo: AssetInfoInternalResponse) =
        CoroutineScope(Dispatchers.IO).async {
            if (assetInfo.externalResponse != null) {
                eventDispatcher.dispatchEvent(
                    UplynkAssetInfoResponseEventImpl(
                        Date(),
                        assetInfo.externalResponse!!
                    )
                )
            } else {
                eventDispatcher.dispatchEvent(
                    UplynkAssetInfoErrorResponseEventImpl(
                        Date(),
                        assetInfo.body,
                        assetInfo.error
                    )
                )
            }
        }
}
