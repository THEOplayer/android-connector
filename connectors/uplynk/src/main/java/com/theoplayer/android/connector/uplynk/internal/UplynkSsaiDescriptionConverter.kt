package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription

internal class UplynkSsaiDescriptionConverter {
    private val DEFAULT_PREFIX = "https://content.uplynk.com"

    fun buildPreplayUrl(ssaiDescription: UplynkSsaiDescription): String = with(ssaiDescription) {
        val prefix = prefix ?: DEFAULT_PREFIX
        val assetIds = when {
            assetIds.isEmpty() && externalId.size == 1 -> "$userId/${externalId.first()}.json"
            assetIds.isEmpty() && externalId.size > 1 -> "$userId/${externalId.joinToString(",")}/multiple.json"
            assetIds.size == 1 -> "${assetIds.first()}.json"
            else -> assetIds.joinToString(separator = ",") + "/multiple.json"
        }

        var url = "$prefix/preplay/$assetIds?v=2"
        if (ssaiDescription.contentProtected) {
            url += "&manifest=mpd"
            url += "&rmt=wv"
        }

        val parameters = preplayParameters.map { "${it.key}=${it.value}" }.joinToString("&")
        url += "&$parameters"

        return url
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
}
