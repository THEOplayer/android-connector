package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.UplynkAssetType
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription

internal val UplynkSsaiDescription.drmParameters: List<Pair<String, String>>
    get() = if (contentProtected) {
        listOf(
            "manifest" to "mpd",
            "rmt" to "wv",
        )
    } else {
        listOf()
    }

internal val UplynkSsaiDescription.pingParameters: List<Pair<String, String>>
    get() {
        val feature = UplynkPingFeatures.from(this)
        return if (feature == UplynkPingFeatures.NO_PING) {
            listOf()
        } else {
            listOf(
                "ad.cping" to "1",
                "ad.pingf" to feature.pingfValue.toString()
            )
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
