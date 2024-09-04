package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.drm.DRMConfiguration

internal fun SourceDescription.replaceSources(sources: List<TypedSource>): SourceDescription {
    return SourceDescription.Builder(*sources.toTypedArray()).apply {
        ads(*ads.toTypedArray())
        textTracks(*textTracks.toTypedArray())
        poster?.let { poster(it) }
        metadata?.let { metadata(it) }
        timeServer?.let { timeServer(it) }
    }.build()
}

internal fun TypedSource.replaceSrc(src: String): TypedSource {
    return TypedSource.Builder(src).apply {
        drm?.let { drm(it) }
        type?.let { type(it) }
        liveOffset?.let { liveOffset(it) }
        ssai?.let { ssai(it) }
        isHlsDateRange?.let { hlsDateRange(it) }
        timeServer?.let { timeServer(it) }
        isLowLatency?.let { lowLatency(it) }
        hls?.let { hls(it) }
        dash?.let { dash(it) }
    }.build()
}

internal fun TypedSource.replaceDrm(drm: DRMConfiguration): TypedSource {
    return TypedSource.Builder(src).apply {
        drm(drm)
        type?.let { type(it) }
        liveOffset?.let { liveOffset(it) }
        ssai?.let { ssai(it) }
        isHlsDateRange?.let { hlsDateRange(it) }
        timeServer?.let { timeServer(it) }
        isLowLatency?.let { lowLatency(it) }
        hls?.let { hls(it) }
        dash?.let { dash(it) }
    }.build()
}