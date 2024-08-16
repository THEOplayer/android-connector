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
        val parameters = preplayParameters.map{ "${it.key}=${it.value}" }.joinToString("&")
        return "$prefix/preplay/$assetIds?v=2&$parameters"
    }

    fun buildAssetInfoUrls(ssaiDescription: UplynkSsaiDescription, sessionId: String): List<String> = with(ssaiDescription) {
        val prefix = prefix ?: DEFAULT_PREFIX
        val urlList = when {
            assetIds.isNotEmpty() -> assetIds.map {
                "$prefix/player/assetinfo/$it.json"
            }
            externalId.isNotEmpty() -> externalId.map {
                "$prefix/player/assetinfo/ext/$userId/$it.json"
            }
            else -> listOf()
        }
        return when {
            sessionId.isBlank() -> urlList
            else -> urlList.map { "$it?pbs=$sessionId" }
        }
    }
}
