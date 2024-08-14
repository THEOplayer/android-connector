package com.theoplayer.android.connector.uplynk.internal

import android.os.Handler
import android.os.Looper
import com.theoplayer.android.connector.uplynk.UplynkListener
import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import com.theoplayer.android.connector.uplynk.network.PreplayResponse
import java.util.concurrent.CopyOnWriteArrayList

internal class UplynkEventDispatcher(val handler: Handler = Handler(Looper.getMainLooper())) {
    private val listeners = CopyOnWriteArrayList<UplynkListener>()

    fun dispatchPreplayEvents(response: PreplayResponse) = listeners.forEach { listener ->
        handler.post { listener.onPreplayResponse(response) }
    }

    fun dispatchAssetInfoEvents(assetInfo: AssetInfoResponse) = listeners.forEach { listener ->
        handler.post { listener.onAssetInfoResponse(assetInfo) }
    }

    fun addListener(listener: UplynkListener) = listeners.add(listener)

    fun removeListener(listener: UplynkListener) = listeners.remove(listener)
}
