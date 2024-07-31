package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription

internal class UplynkSsaiDescriptionConverter {
    private val DEFAULT_PREFIX = "https://content.uplynk.com"

    fun buildPreplayUrl(ssaiDescription: UplynkSsaiDescription): String = with(ssaiDescription) {
        val prefix = prefix ?: DEFAULT_PREFIX
        val assetIds = if (assetIds.size == 1) {
            "${assetIds.first()}.json"
        } else {
            assetIds.joinToString(separator = ",") + "/multiple.json"
        }
        val parameters = preplayParameters.map{ "${it.key}=${it.value}" }.joinToString("&")
        return "$prefix/preplay/$assetIds?v=2&$parameters"
    }
}
