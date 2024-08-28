package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import com.theoplayer.android.connector.uplynk.network.PreplayResponse
import com.theoplayer.android.connector.uplynk.network.UplynkAd
import com.theoplayer.android.connector.uplynk.network.UplynkAdBreak
import com.theoplayer.android.connector.uplynk.network.UplynkAds

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
     * Called when ad break data is updated.
     *
     * @param ads The updated ad break data.
     */
    fun onAdBreaksUpdated(ads: UplynkAds) {}

    /**
     * Called when an individual ad begins.
     *
     * @param ad The ad that is starting.
     */
    fun onAdBegin(ad: UplynkAd) {}

    /**
     * Called when an individual ad ends.
     *
     * @param ad The ad that has finished.
     */
    fun onAdEnd(ad: UplynkAd) {}

    /**
     * Called when an ad break begins.
     *
     * @param adBreak The ad break that is starting.
     */
    fun onAdBreakBegin(adBreak: UplynkAdBreak) {}

    /**
     * Called when an ad break ends.
     *
     * @param adBreak The ad break that has ended.
     */
    fun onAdBreakEnd(adBreak: UplynkAdBreak) {}
}
