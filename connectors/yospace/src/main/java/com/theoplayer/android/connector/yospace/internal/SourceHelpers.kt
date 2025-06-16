package com.theoplayer.android.connector.yospace.internal

import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource

internal fun SourceDescription.replaceSources(sources: List<TypedSource>): SourceDescription {
    return copy(sources = sources)
}

internal fun TypedSource.replaceSrc(src: String): TypedSource {
    return copy(src = src)
}