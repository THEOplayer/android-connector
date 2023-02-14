package com.theoplayer.android.connector.analytics.conviva.utils

import com.conviva.sdk.ConvivaSdkConstants
import com.theoplayer.android.api.THEOplayerGlobal
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.GoogleImaAd
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.connector.analytics.conviva.ConvivaConfiguration
import com.theoplayer.android.connector.analytics.conviva.ConvivaMetadata

fun calculateCurrentAdBreakInfo(adBreak: AdBreak, adBreakIndex: Int): Map<String, Any> {
    val timeOffset = adBreak.timeOffset
    val adPosition: ConvivaSdkConstants.AdPosition = when {
        timeOffset == 0 -> ConvivaSdkConstants.AdPosition.PREROLL
        timeOffset > 0 -> ConvivaSdkConstants.AdPosition.MIDROLL
        else -> ConvivaSdkConstants.AdPosition.POSTROLL
    }
    return mapOf(
        ConvivaSdkConstants.POD_POSITION to adPosition,
        ConvivaSdkConstants.POD_DURATION to adBreak.maxDuration,
        ConvivaSdkConstants.POD_INDEX to adBreakIndex
    )
}

fun calculateConvivaOptions(config: ConvivaConfiguration): Map<String, Any> {
    // No need to set GATEWAY_URL and LOG_LEVEL settings for your production release.
    // The Conviva SDK provides the default values for production
    val options = mutableMapOf<String, Any>()
    if (config.debug == true) {
        options[ConvivaSdkConstants.LOG_LEVEL] = ConvivaSdkConstants.LogLevel.DEBUG
    }
    // GATEWAY_URL: once enabled, your Conviva data will appear in Touchstone
    if (config.gatewayUrl != null) {
        options[ConvivaSdkConstants.GATEWAY_URL] = config.gatewayUrl
    }
    return options
}

fun collectPlayerInfo(): Map<String, Any> {
    return mapOf(
        ConvivaSdkConstants.FRAMEWORK_NAME to "THEOplayer",
        ConvivaSdkConstants.FRAMEWORK_VERSION to THEOplayerGlobal.getVersion()
    )
}

fun collectContentMetadata(
    player: Player,
    configuredContentMetadata: ConvivaMetadata
): ConvivaMetadata {
    // Never send an Infinity or NaN
    val duration = player.duration
    return if (duration.isNaN() || duration.isInfinite())
        configuredContentMetadata
    else
        configuredContentMetadata + mapOf(ConvivaSdkConstants.DURATION to duration.toInt())
}

fun collectAdMetadata(ad: GoogleImaAd): ConvivaMetadata {
    return mapOf(
        ConvivaSdkConstants.DURATION to ad.imaAd.duration.toInt(),
        ConvivaSdkConstants.STREAM_URL to ad.imaAd.adId,
        ConvivaSdkConstants.ASSET_NAME to (ad.imaAd.title ?: ad.id)
    )
}

/**
 * Calculate length of current buffer.
 * Only take into account the buffered range around the currentTime.
 */
fun calculateBufferLength(player: Player): Long {
    var bufferLength = 0.0
    val currentTime = player.currentTime
    player.buffered.forEach { range ->
        val start = range.start
        val end = range.end
        if (start <= currentTime && currentTime < end) {
            bufferLength += end - currentTime
        }
    }
    return (1e3 * bufferLength).toLong()
}