package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.UplynkAssetType
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import kotlin.time.Duration

private const val AD_IMPRESSIONS = 1
private const val FW_VIDEO_VIEWS = 2
private const val LINEAR_AD_DATA = 4

internal class UplynkSsaiDescriptionConverter {
    private val DEFAULT_PREFIX = "https://content.uplynk.com"

    fun buildPreplayVodUrl(ssaiDescription: UplynkSsaiDescription): String = with(ssaiDescription) {
        val prefix = prefix ?: DEFAULT_PREFIX

        var url = "$prefix/preplay/$urlAssetId?v=2"
        if (ssaiDescription.contentProtected) {
            url += "&manifest=mpd"
            url += "&rmt=wv"
        }

        url += "&$pingParameters&$urlParameters"

        return url
    }

    fun buildPreplayLiveUrl(ssaiDescription: UplynkSsaiDescription): String = with(ssaiDescription) {
        val prefix = prefix ?: DEFAULT_PREFIX

        var url = "$prefix/preplay/$urlAssetType/$urlAssetId?v=2"
        if (ssaiDescription.contentProtected) {
            url += "&manifest=mpd"
            url += "&rmt=wv"
        }

        url += "&$pingParameters&$urlParameters"

        return url
    }

    private val UplynkSsaiDescription.urlParameters
        get() = preplayParameters.map { "${it.key}=${it.value}" }.joinToString("&")

    private val UplynkSsaiDescription.pingParameters: String
        get() {
            val isLive = assetType == UplynkAssetType.ASSET

            val features = with(pingConfiguration) {
                (AD_IMPRESSIONS.takeIf { !isLive && adImpressions } ?: 0) +
                        (FW_VIDEO_VIEWS.takeIf { !isLive && freeWheelVideoViews } ?: 0) +
                        (LINEAR_AD_DATA.takeIf { isLive && linearAdData } ?: 0)
            }
            return if (features == 0) {
                "ad.pingc=0"
            } else {
                "ad.pingc=1&ad.pingf=$features"
            }
        }

    private val UplynkSsaiDescription.urlAssetType
        get() = when (assetType) {
            UplynkAssetType.ASSET -> ""
            UplynkAssetType.CHANNEL -> "channel"
            UplynkAssetType.EVENT -> "event"
        }

    private val UplynkSsaiDescription.urlAssetId
        get() = when {
            assetIds.isEmpty() && externalId.size == 1 -> "$userId/${externalId.first()}.json"
            assetIds.isEmpty() && externalId.size > 1 -> "$userId/${externalId.joinToString(",")}/multiple.json"
            assetIds.size == 1 -> "${assetIds.first()}.json"
            else -> assetIds.joinToString(separator = ",") + "/multiple.json"
        }

    fun buildAssetInfoUrls(
        ssaiDescription: UplynkSsaiDescription,
        sessionId: String,
        prefix: String
    ): List<String> = with(ssaiDescription) {
        val urlList = when {
            assetIds.isNotEmpty() -> assetIds.map {
                "$prefix/player/assetinfo/$it.json"
            }

            externalId.isNotEmpty() -> externalId.map {
                "$prefix/player/assetinfo/ext/$userId/$it.json"
            }

            else -> emptyList()
        }
        return if (sessionId.isBlank()) {
            urlList
        } else {
            urlList.map { "$it?pbs=$sessionId" }
        }
    }

    fun buildSeekedPingUrl(
        prefix: String, sessionId: String, currentTime: Duration, seekStartTime: Duration
    ) = buildPingUrl(prefix, sessionId, currentTime) + "&ev=seek&ft=${seekStartTime.inWholeSeconds}"

    fun buildStartPingUrl(
        prefix: String, sessionId: String, currentTime: Duration
    ) = buildPingUrl(prefix, sessionId, currentTime) + "&ev=start"

    fun buildPingUrl(
        prefix: String, sessionId: String, currentTime: Duration
    ) = "$prefix/session/ping/$sessionId.json?v=3&pt=${currentTime.inWholeSeconds}"
}
