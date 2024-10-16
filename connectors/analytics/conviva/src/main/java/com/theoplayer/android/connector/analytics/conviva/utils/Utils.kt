package com.theoplayer.android.connector.analytics.conviva.utils

import com.conviva.sdk.ConvivaSdkConstants
import com.theoplayer.android.api.THEOplayerGlobal
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.GoogleImaAd
import com.theoplayer.android.api.error.THEOplayerException
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.connector.analytics.conviva.ConvivaConfiguration
import com.theoplayer.android.connector.analytics.conviva.ConvivaMetadata

fun calculateAdType(player: Player): ConvivaSdkConstants.AdType {
    return if (player.source?.ads.isNullOrEmpty())
        ConvivaSdkConstants.AdType.SERVER_SIDE
    else
        ConvivaSdkConstants.AdType.CLIENT_SIDE
}

fun calculateAdTypeAsString(player: Player): String {
    return when (calculateAdType(player)) {
        ConvivaSdkConstants.AdType.SERVER_SIDE -> "Server Side"
        ConvivaSdkConstants.AdType.CLIENT_SIDE -> "Client Side"
    }
}

fun calculateCurrentAdBreakPosition(adBreak: AdBreak): String {
    val timeOffset = adBreak.timeOffset
    return when {
        timeOffset == 0 -> "Pre-roll"
        timeOffset > 0 -> "Mid-roll"
        else -> "Post-roll"
    }
}

fun calculateCurrentAdBreakInfo(adBreak: AdBreak, adBreakIndex: Int): Map<String, Any> {
    return mapOf(
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

private fun validStringOrNA(str: String?): String {
    return if (str.isNullOrEmpty()) "NA" else str
}

private fun validStringOrFallbackOrNA(str: String?, fbStr: String?): String {
    return if (!str.isNullOrEmpty())
        str
    else if (!fbStr.isNullOrEmpty())
        fbStr
    else
        "NA"
}

fun collectAdMetadata(ad: GoogleImaAd): ConvivaMetadata {
    // AssetName should not never be an empty string
    val assetName = validStringOrFallbackOrNA(ad.imaAd.title, ad.id)
    return mutableMapOf(
        ConvivaSdkConstants.DURATION to ad.imaAd.duration.toInt(),
        ConvivaSdkConstants.ASSET_NAME to assetName,

        // [Required] This Ad ID is from the Ad Server that actually has the ad creative.
        // For wrapper ads, this is the last Ad ID at the end of the wrapper chain.
        "c3.ad.id" to ad.id,

        // [Required] The creative name (may be the same as the ad name) as a string.
        // Creative name is available from the ad server. Set to "NA" if not available.
        "adMetadata" to assetName,

        // [Required] The creative id of the ad. This creative id is from the Ad Server that actually has the ad creative.
        // For wrapper ads, this is the last creative id at the end of the wrapper chain. Set to "NA" if not available.
        "c3.ad.creativeId" to validStringOrNA(ad.creativeId),

        // [Preferred] A string that identifies the Ad System (i.e. the Ad Server). This Ad System represents
        // the Ad Server that actually has the ad creative. For wrapper ads, this is the last Ad System at the end of
        // the wrapper chain. Set to "NA" if not available
        "c3.ad.system" to validStringOrNA(ad.adSystem),

        // [Preferred] A boolean value that indicates whether this ad is a Slate or not.
        // Set to "true" for Slate and "false" for a regular ad. By default, set to "false"
        "c3.ad.isSlate" to "false",

        // [Preferred] Only valid for wrapper VAST responses.
        // This tag must capture the "first" Ad Id in the wrapper chain when a Linear creative is available or there is
        // an error at the end of the wrapper chain. Set to "NA" if not available. If there is no wrapper VAST response
        // then the Ad Id and First Ad Id should be the same.
        "c3.ad.firstAdId" to (ad.wrapperAdIds.firstOrNull() ?: ad.id),

        // [Preferred] Only valid for wrapper VAST responses.
        // This tag must capture the "first" Creative Id in the wrapper chain when a Linear creative is available or
        // there is an error at the end of the wrapper chain. Set to "NA" if not available. If there is no wrapper
        // VAST response then the Ad Creative Id and First Ad Creative Id should be the same.
        "c3.ad.firstCreativeId" to validStringOrNA(
            ad.wrapperCreativeIds.firstOrNull() ?: ad.creativeId
        ),

        // [Preferred] Only valid for wrapper VAST responses. This tag must capture the "first" Ad System in the wrapper
        // chain when a Linear creative is available or there is an error at the end of the wrapper chain. Set to "NA" if
        // not available. If there is no wrapper VAST response then the Ad System and First Ad System should be the same.
        // Examples: "GDFP", "NA".
        "c3.ad.firstAdSystem" to validStringOrNA(ad.wrapperAdSystems.firstOrNull() ?: ad.adSystem),

        // The name of the Ad Stitcher. If not using an Ad Stitcher, set to "NA"
        "c3.ad.adStitcher" to "NA"
    ).apply {

        // [Required] The ad position as a string "Pre-roll", "Mid-roll" or "Post-roll"
        ad.adBreak?.let { adBreak ->
            this["c3.ad.position"] = calculateCurrentAdBreakPosition(adBreak)
        }
    }
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

fun flattenErrorObject(error: THEOplayerException): Map<String, String> {
    return mapOf(
        "code" to error.code.name,
        "category" to error.category.name,
        "stack" to (error.stackTraceToString()),
        "cause.stack" to (error.cause?.stackTraceToString() ?: ""),
        "cause.message" to (error.cause?.message ?: "")
    ).filterValues { it != "" } // Remove entries with empty values
}
