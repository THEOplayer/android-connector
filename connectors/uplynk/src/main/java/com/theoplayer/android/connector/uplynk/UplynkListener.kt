package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.connector.uplynk.internal.network.PingResponse
import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import com.theoplayer.android.connector.uplynk.network.PreplayLiveResponse
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
     * Called when a preplay response is received from Uplynk for live channel or an event.
     *
     * For more details, refer to the [Preplay API (Version 2) Documentation](https://docs.edgecast.com/video/index.html#Develop/Preplayv2.htm).
     *
     * @param response the `PreplayLiveResponse` object containing information relevant to the preplay request.
     */
    fun onPreplayLiveResponse(response: PreplayLiveResponse){}

    /**
     * Called when a preplay response is received from Uplynk and failed to be parsed
     *
     * @param exception the `Exception` occurred during the response parsing
     */
    fun onPreplayFailure(exception: Exception) {}

    /**
     * Called when an asset information response is received from Uplynk.
     *
     * For more details, refer to the [Asset Info Documentation](https://docs.edgecast.com/video/index.html#Develop/AssetInfo.htm).
     *
     * @param response the `AssetInfoResponse` object containing detailed information about the asset.
     */
    fun onAssetInfoResponse(response: AssetInfoResponse) {}

    /**
     * Called when an asset information response is failed
     *
     * @param exception the `Exception` occurred during the request
     */
    fun onAssetInfoFailure(exception: Exception) {}

    /**
     * Called when a ping response is received from Uplynk.
     *
     * For more details, refer to the [Ping API Documentation](https://docs.edgecast.com/video/#Develop/Pingv2.htm).
     *
     * @param pingResponse the `PingResponse` object containing ping request result
     */
    fun onPingResponse(pingResponse: PingResponse) {}
}
