package com.theoplayer.android.connector.uplynk.internal

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

    fun buildPlaybackUrl(playUrl: String, ssaiDescription: UplynkSsaiDescription): String = with(ssaiDescription) {
        return "$playUrl$playUrlParameters"
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
