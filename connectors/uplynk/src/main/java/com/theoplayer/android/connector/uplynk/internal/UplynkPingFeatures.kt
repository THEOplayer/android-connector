package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.UplynkAssetType
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription

enum class UplynkPingFeatures(val pingfValue: Int) {
    NO_PING(0),
    AD_IMPRESSIONS(1),
    FW_VIDEO_VIEWS(2),
    AD_IMPRESSIONS_AND_FW_VIDEO_VIEWS(3),
    LINEAR_AD_DATA(4);

    companion object {
        fun from(ssaiDescription: UplynkSsaiDescription): UplynkPingFeatures {
            val isVod = ssaiDescription.assetType == UplynkAssetType.ASSET
            with(ssaiDescription.pingConfiguration) {
                return when {
                    isVod && adImpressions && freeWheelVideoViews -> AD_IMPRESSIONS_AND_FW_VIDEO_VIEWS
                    isVod && adImpressions -> AD_IMPRESSIONS
                    isVod && freeWheelVideoViews -> FW_VIDEO_VIEWS
                    !isVod && linearAdData -> LINEAR_AD_DATA
                    else -> NO_PING
                }
            }
        }
    }

}