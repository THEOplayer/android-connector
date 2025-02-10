package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.UplynkAssetType
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription

internal val UplynkSsaiDescription.drmParameters: String
    get() = if (contentProtected) {
        "&manifest=mpd&rmt=wv"
    } else {
        ""
    }

internal val UplynkSsaiDescription.urlParameters
    get() = if (preplayParameters.isNotEmpty()) {
        preplayParameters.map { "${it.key}=${it.value}" }.joinToString("&", prefix = "&")
    } else {
        ""
    }

internal val UplynkSsaiDescription.pingParameters: String
    get() {
        val feature = UplynkPingFeatures.from(this)
        return if (feature == UplynkPingFeatures.NO_PING) {
            "&ad.pingc=0"
        } else {
            "&ad.pingc=1&ad.pingf=${feature.pingfValue}"
        }
    }

internal val UplynkSsaiDescription.urlAssetType
    get() = when (assetType) {
        UplynkAssetType.ASSET -> ""
        UplynkAssetType.CHANNEL -> "channel"
        UplynkAssetType.EVENT -> "event"
    }

internal val UplynkSsaiDescription.urlAssetId
    get() = when {
        assetIds.isEmpty() && externalIds.size == 1 -> "ext/$userId/${externalIds.first()}.json"
        assetIds.isEmpty() && externalIds.size > 1 -> "ext/$userId/${externalIds.joinToString(",")}/multiple.json"
        assetIds.size == 1 -> "${assetIds.first()}.json"
        else -> assetIds.joinToString(separator = ",") + "/multiple.json"
    }
