package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import com.theoplayer.android.connector.uplynk.network.PreplayResponse

interface UplynkListener {
    fun onPreplayResponse(response: PreplayResponse)
    fun onAssetInfoResponse(response: AssetInfoResponse)

}
