package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.drm.DRMConfiguration

internal fun SourceDescription.replaceSources(sources: List<TypedSource>): SourceDescription {
    return copy(sources = sources)
}

internal fun TypedSource.replaceSrc(src: String): TypedSource {
    return copy(src = src)
}

internal fun TypedSource.replaceDrm(drm: DRMConfiguration): TypedSource {
    return copy(drm = drm)
}