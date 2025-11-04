package com.theoplayer.android.connector.analytics.conviva.utils

import androidx.core.net.toUri
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaSdkConstants.StreamType
import com.theoplayer.android.api.THEOplayerGlobal
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.LinearAd
import com.theoplayer.android.api.ads.ima.GoogleImaAd
import com.theoplayer.android.api.ads.theoads.TheoAdDescription
import com.theoplayer.android.api.event.ads.AdIntegrationKind
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.timerange.TimeRanges
import com.theoplayer.android.connector.analytics.conviva.ConvivaConfiguration
import com.theoplayer.android.connector.analytics.conviva.ConvivaMetadata
import java.lang.Double.isFinite

fun calculateAdType(ad: Ad): ConvivaSdkConstants.AdType {
    return when (ad.integration) {
        // TODO THEOads is a SGAI solution which can't be reported to Conviva as such yet.
        AdIntegrationKind.THEO_ADS -> ConvivaSdkConstants.AdType.SERVER_SIDE
        AdIntegrationKind.GOOGLE_IMA -> ConvivaSdkConstants.AdType.CLIENT_SIDE
        else -> ConvivaSdkConstants.AdType.SERVER_SIDE
    }
}

fun calculateAdType(adBreak: AdBreak): ConvivaSdkConstants.AdType {
    return when (adBreak.integration) {
        // TODO THEOads is a SGAI solution which can't be reported to Conviva as such yet.
        AdIntegrationKind.THEO_ADS -> ConvivaSdkConstants.AdType.SERVER_SIDE
        AdIntegrationKind.GOOGLE_IMA -> ConvivaSdkConstants.AdType.CLIENT_SIDE
        else -> ConvivaSdkConstants.AdType.SERVER_SIDE
    }
}

fun calculateAdTypeAsString(ad: Ad): String {
    if (ad.integration == AdIntegrationKind.THEO_ADS) {
        return "Server Guided"
    }
    return when (calculateAdType(ad)) {
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

fun calculateConvivaOptions(config: ConvivaConfiguration): Map<String, Any> = buildMap {
    // No need to set GATEWAY_URL and LOG_LEVEL settings for your production release.
    // The Conviva SDK provides the default values for production
    if (config.debug == true) {
        put(ConvivaSdkConstants.LOG_LEVEL, ConvivaSdkConstants.LogLevel.DEBUG)
    }
    // GATEWAY_URL: once enabled, your Conviva data will appear in Touchstone
    config.gatewayUrl?.let { put(ConvivaSdkConstants.GATEWAY_URL, it) }
}

fun calculateStreamType(player: Player): StreamType? {
    return if (!player.duration.isNaN()) {
        if (isFinite(player.duration)) {
            StreamType.VOD
        } else {
            StreamType.LIVE
        }
    } else {
        null
    }
}

fun calculateEncodingType(source: TypedSource?): String? {
    return when (source?.type) {
        SourceType.DASH -> "DASH"
        SourceType.HLS, SourceType.HLSX -> "HLS"
        SourceType.HESP -> "HESP"
        else -> {
            // No type given, check for known extension.
            source?.src?.toUri()?.lastPathSegment?.let { pathSegment ->
                when {
                    pathSegment.endsWith(".mpd") -> "DASH"
                    pathSegment.endsWith(".m3u8") -> "HLS"
                    else -> null
                }
            }
        }
    }
}

fun collectPlayerInfo(): Map<String, Any> {
    return mapOf(
        ConvivaSdkConstants.FRAMEWORK_NAME to "THEOplayer",
        ConvivaSdkConstants.FRAMEWORK_VERSION to THEOplayerGlobal.getVersion()
    )
}

fun collectPlaybackConfigMetadata(player: Player): ConvivaMetadata = buildMap {
    put("targetBuffer", player.abr.targetBuffer)
    put("abrStrategy", player.abr.abrStrategy.type.name.lowercase())
    player.abr.abrStrategy.metadata?.bitrate?.let { abrMetadata ->
        put("abrMetadata", abrMetadata)
    }
    player.source?.sources?.firstOrNull()?.liveOffset?.let { liveOffset ->
        put("liveOffset", liveOffset)
    }
}

fun collectAdDescriptionMetadata(player: Player): Map<String, String> {
    return mutableMapOf<String, String>().apply {
        player.source?.ads?.firstNotNullOfOrNull { (it as? TheoAdDescription)?.streamActivityMonitorId }
            ?.let {
                put("streamActivityMonitorId", it)
            }
    }
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

fun updateAdMetadataForGoogleIma(ad: GoogleImaAd, metadata: ConvivaMetadata): ConvivaMetadata {
    val assetName = validStringOrFallbackOrNA(ad.imaAd.title, ad.id)
    val googleImaMetadataMap = mutableMapOf(
        ConvivaSdkConstants.DURATION to ad.imaAd.duration.toInt(),
        ConvivaSdkConstants.ASSET_NAME to assetName,
        "adMetadata" to assetName,
        "c3.ad.creativeId" to validStringOrNA(ad.creativeId),
        "c3.ad.system" to validStringOrNA(ad.adSystem),
        "c3.ad.firstAdId" to (ad.wrapperAdIds.firstOrNull() ?: ad.id),
        "c3.ad.firstCreativeId" to validStringOrNA(
            ad.wrapperCreativeIds.firstOrNull() ?: ad.creativeId
        ),
        "c3.ad.firstAdSystem" to validStringOrNA(ad.wrapperAdSystems.firstOrNull() ?: ad.adSystem),
    )
    return metadata + googleImaMetadataMap
}

fun collectAdMetadata(ad: Ad): ConvivaMetadata {
    // AssetName should never be an empty string
    return mutableMapOf(
        ConvivaSdkConstants.DURATION to if (ad is LinearAd) ad.duration else 0,
        ConvivaSdkConstants.ASSET_NAME to ad.id,

        // [Required] This Ad ID is from the Ad Server that actually has the ad creative.
        // For wrapper ads, this is the last Ad ID at the end of the wrapper chain.
        "c3.ad.id" to ad.id,

        // [Required] The creative name (may be the same as the ad name) as a string.
        // Creative name is available from the ad server. Set to "NA" if not available.
        "adMetadata" to ad.id,

        // [Required] The creative id of the ad. This creative id is from the Ad Server that actually has the ad creative.
        // For wrapper ads, this is the last creative id at the end of the wrapper chain. Set to "NA" if not available.
        "c3.ad.creativeId" to "NA",

        // [Preferred] A string that identifies the Ad System (i.e. the Ad Server). This Ad System represents
        // the Ad Server that actually has the ad creative. For wrapper ads, this is the last Ad System at the end of
        // the wrapper chain. Set to "NA" if not available
        "c3.ad.system" to "NA",

        // [Preferred] A boolean value that indicates whether this ad is a Slate or not.
        // Set to "true" for Slate and "false" for a regular ad. By default, set to "false"
        "c3.ad.isSlate" to "false",

        // [Preferred] Only valid for wrapper VAST responses.
        // This tag must capture the "first" Ad Id in the wrapper chain when a Linear creative is available or there is
        // an error at the end of the wrapper chain. Set to "NA" if not available. If there is no wrapper VAST response
        // then the Ad Id and First Ad Id should be the same.
        "c3.ad.firstAdId" to ad.id,

        // [Preferred] Only valid for wrapper VAST responses.
        // This tag must capture the "first" Creative Id in the wrapper chain when a Linear creative is available or
        // there is an error at the end of the wrapper chain. Set to "NA" if not available. If there is no wrapper
        // VAST response then the Ad Creative Id and First Ad Creative Id should be the same.
        "c3.ad.firstCreativeId" to "NA",

        // [Preferred] Only valid for wrapper VAST responses. This tag must capture the "first" Ad System in the wrapper
        // chain when a Linear creative is available or there is an error at the end of the wrapper chain. Set to "NA" if
        // not available. If there is no wrapper VAST response then the Ad System and First Ad System should be the same.
        // Examples: "GDFP", "NA".
        "c3.ad.firstAdSystem" to "NA",

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

fun bufferedToString(buffered: TimeRanges): String {
    return "[${buffered.joinToString(",") { timeRange -> "${timeRange.start}-${timeRange.end}" }}]"
}
