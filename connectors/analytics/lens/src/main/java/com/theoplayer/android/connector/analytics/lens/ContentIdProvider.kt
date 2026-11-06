package com.theoplayer.android.connector.analytics.lens

import com.theoplayer.android.api.source.SourceDescription

/**
 * Provides the content ID to report for a source.
 */
fun interface ContentIdProvider {
    /**
     * Returns the content ID (1-128 printable ASCII) to report for [source], or null to let Lens derive one from the
     * source URL.
     */
    fun contentId(source: SourceDescription): String?
}
