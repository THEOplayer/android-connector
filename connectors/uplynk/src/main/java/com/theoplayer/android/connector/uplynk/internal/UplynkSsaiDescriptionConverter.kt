package com.theoplayer.android.connector.uplynk.internal

import android.net.Uri
import androidx.core.net.toUri
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import kotlin.time.Duration

internal class UplynkSsaiDescriptionConverter {
    private val DEFAULT_PREFIX = "https://content.uplynk.com"

    fun buildPreplayVodUrl(ssaiDescription: UplynkSsaiDescription): Uri = with(ssaiDescription) {
        val prefix = prefix ?: DEFAULT_PREFIX

        return "$prefix/preplay/$urlAssetId?v=2$drmParameters$pingParameters$urlParameters".toUri()
    }

    fun buildPreplayLiveUrl(ssaiDescription: UplynkSsaiDescription): Uri = with(ssaiDescription) {
        val prefix = prefix ?: DEFAULT_PREFIX

        return "$prefix/preplay/$urlAssetType/$urlAssetId?v=2$drmParameters$pingParameters$urlParameters".toUri()
    }

    fun buildPlaybackUrl(playUrl: String, ssaiDescription: UplynkSsaiDescription): Uri = with(ssaiDescription) {
        return "$playUrl$playUrlParameters".toUri()
    }

    fun buildAssetInfoUrls(
        ssaiDescription: UplynkSsaiDescription,
        sessionId: String,
        prefix: String
    ): List<Uri> = with(ssaiDescription) {
        val urlList = when {
            assetIds.isNotEmpty() -> assetIds.map {
                "$prefix/player/assetinfo/$it.json".toUri()
            }

            externalIds.isNotEmpty() -> externalIds.map {
                "$prefix/player/assetinfo/ext/$userId/$it.json".toUri()
            }

            else -> emptyList()
        }
        return if (sessionId.isBlank()) {
            urlList
        } else {
            urlList.map { "$it?pbs=$sessionId".toUri() }
        }
    }

    fun buildSeekedPingUrl(
        prefix: String, sessionId: String, currentTime: Duration, seekStartTime: Duration
    ) = (buildPingUrl(prefix, sessionId, currentTime).toString() + "&ev=seek&ft=${seekStartTime.inWholeSeconds}").toUri()

    fun buildStartPingUrl(
        prefix: String, sessionId: String, currentTime: Duration
    ) = (buildPingUrl(prefix, sessionId, currentTime).toString() + "&ev=start").toUri()

    fun buildPingUrl(
        prefix: String, sessionId: String, currentTime: Duration
    ) = "$prefix/session/ping/$sessionId.json?v=3&pt=${currentTime.inWholeSeconds}".toUri()
}
