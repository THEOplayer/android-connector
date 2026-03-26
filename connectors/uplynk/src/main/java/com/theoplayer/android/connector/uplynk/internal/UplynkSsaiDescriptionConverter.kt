package com.theoplayer.android.connector.uplynk.internal

import android.net.Uri
import androidx.core.net.toUri
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import kotlin.time.Duration

internal class UplynkSsaiDescriptionConverter {
    private val DEFAULT_PREFIX = "https://content.uplynk.com"

    fun buildPreplayVodUrl(ssaiDescription: UplynkSsaiDescription): Uri =
        with(ssaiDescription) {
            return (prefix ?: DEFAULT_PREFIX).toUri().buildUpon().apply {
                appendEncodedPath("preplay/$urlAssetId")
                appendQueryParameter("v", "2")
                drmParameters.forEach { (key, value) -> appendQueryParameter(key, value) }
                pingParameters.forEach { (key, value) -> appendQueryParameter(key, value) }
                preplayParameters.forEach { (key, value) -> appendQueryParameter(key, value) }
            }.build()
        }

    fun buildPreplayLiveUrl(ssaiDescription: UplynkSsaiDescription): Uri =
        with(ssaiDescription) {
            return (prefix ?: DEFAULT_PREFIX).toUri().buildUpon().apply {
                appendEncodedPath("preplay/$urlAssetType/$urlAssetId")
                appendQueryParameter("v", "2")
                drmParameters.forEach { (key, value) -> appendQueryParameter(key, value) }
                pingParameters.forEach { (key, value) -> appendQueryParameter(key, value) }
                preplayParameters.forEach { (key, value) -> appendQueryParameter(key, value) }
            }.build()
        }

    fun buildPlaybackUrl(playUrl: String, ssaiDescription: UplynkSsaiDescription): Uri =
        with(ssaiDescription) {
            return playUrl.toUri().buildUpon().apply {
                playbackUrlParameters.forEach { (key, value) -> appendQueryParameter(key, value) }
            }.build()
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
            urlList.map { url ->
                url.buildUpon().apply {
                    appendQueryParameter("pbs", sessionId)
                }.build()
            }
        }
    }

    fun buildSeekedPingUrl(
        prefix: String,
        sessionId: String,
        currentTime: Duration,
        seekStartTime: Duration
    ): Uri = buildPingUrl(prefix, sessionId, currentTime).buildUpon().apply {
        appendQueryParameter("ev", "seek")
        appendQueryParameter("ft", seekStartTime.inWholeSeconds.toString())
    }.build()

    fun buildStartPingUrl(
        prefix: String,
        sessionId: String,
        currentTime: Duration
    ): Uri = buildPingUrl(prefix, sessionId, currentTime).buildUpon().apply {
        appendQueryParameter("ev", "start")
    }.build()

    fun buildPingUrl(
        prefix: String,
        sessionId: String,
        currentTime: Duration
    ): Uri = prefix.toUri().buildUpon().apply {
        appendEncodedPath("session/ping/$sessionId.json")
        appendQueryParameter("v", "3")
        appendQueryParameter("pt", currentTime.inWholeSeconds.toString())
    }.build()
}
