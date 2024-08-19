package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import com.theoplayer.android.connector.uplynk.network.PreplayResponse

/**
 * A listener interface for receiving events related to Uplynk
 * Implementations of this interface can be used to handle responses from Uplynk's API.
 */
interface UplynkListener {

    /**
     * Called when a preplay response is received from Uplynk.
     *
     * For more details, refer to the [Preplay API (Version 2) Documentation](https://docs.edgecast.com/video/index.html#Develop/Preplayv2.htm).
     *
     * @param response the `PreplayResponse` object containing information relevant to the preplay request.
     */
    fun onPreplayResponse(response: PreplayResponse) {}

    /**
     * Called when an asset information response is received from Uplynk.
     *
     * For more details, refer to the [Asset Info Documentation](https://docs.edgecast.com/video/index.html#Develop/AssetInfo.htm).
     *
     * @param response the `AssetInfoResponse` object containing detailed information about the asset.
     */
    fun onAssetInfoResponse(response: AssetInfoResponse) {}

}
