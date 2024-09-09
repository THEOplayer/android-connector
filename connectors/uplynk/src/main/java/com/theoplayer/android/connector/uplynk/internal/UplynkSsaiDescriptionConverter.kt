package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.UplynkAssetType
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import kotlin.time.Duration

internal class UplynkSsaiDescriptionConverter {
    private val DEFAULT_PREFIX = "https://content.uplynk.com"

    fun buildPreplayVodUrl(ssaiDescription: UplynkSsaiDescription): String = with(ssaiDescription) {
        val prefix = prefix ?: DEFAULT_PREFIX

        return "$prefix/preplay/$urlAssetId?v=2$drmParameters$pingParameters$urlParameters"
    }

    fun buildPreplayLiveUrl(ssaiDescription: UplynkSsaiDescription): String = with(ssaiDescription) {
        val prefix = prefix ?: DEFAULT_PREFIX

        return "$prefix/preplay/$urlAssetType/$urlAssetId?v=2$drmParameters$pingParameters$urlParameters"
    }

    private val UplynkSsaiDescription.drmParameters: String
        get() = if (contentProtected) {
            "&manifest=mpd&rmt=wv"
        } else {
            ""
        }

    private val UplynkSsaiDescription.urlParameters
        get() = if (preplayParameters.isNotEmpty()) {
            preplayParameters.map { "${it.key}=${it.value}" }.joinToString("&", prefix = "&")
        } else {
            ""
        }

    private val UplynkSsaiDescription.pingParameters: String
        get() {
            val feature = UplynkPingFeatures.from(this)
            return if (feature == UplynkPingFeatures.NO_PING) {
                "&ad.pingc=0"
            } else {
                "&ad.pingc=1&ad.pingf=${feature.pingfValue}"
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
            assetIds.isEmpty() && externalIds.size == 1 -> "$userId/${externalIds.first()}.json"
            assetIds.isEmpty() && externalIds.size > 1 -> "$userId/${externalIds.joinToString(",")}/multiple.json"
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

            externalIds.isNotEmpty() -> externalIds.map {
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
