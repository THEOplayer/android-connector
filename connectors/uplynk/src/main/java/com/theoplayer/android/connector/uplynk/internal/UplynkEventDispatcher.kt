package com.theoplayer.android.connector.uplynk.internal

import android.os.Handler
import android.os.Looper
import com.theoplayer.android.connector.uplynk.UplynkListener
import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import com.theoplayer.android.connector.uplynk.network.PreplayResponse
import java.util.concurrent.CopyOnWriteArrayList

internal class UplynkEventDispatcher {
    private val handler = Handler(Looper.getMainLooper())

    private val listeners = CopyOnWriteArrayList<UplynkListener>()

    fun dispatchPreplayEvents(response: PreplayResponse) = handler.post {
        listeners.forEach { it.onPreplayResponse(response) }
    }

    fun dispatchAssetInfoEvents(assetInfo: AssetInfoResponse) = handler.post {
        listeners.forEach { it.onAssetInfoResponse(assetInfo) }
    }

    fun dispatchAssetInfoFailure(e: Exception) = handler.post {
        listeners.forEach { it.onAssetInfoFailure(e) }
    }

    fun dispatchPreplayFailure(e: Exception) = handler.post {
        listeners.forEach { it.onPreplayFailure(e) }
    }

    fun addListener(listener: UplynkListener) = listeners.add(listener)

    fun removeListener(listener: UplynkListener) = listeners.remove(listener)
}
