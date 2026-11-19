package com.theoplayer.android.connector.analytics.lens

import com.theoplayer.android.api.source.SourceDescription

/**
 * Provides the content ID to report for a source.
 */
fun interface ContentIdProvider {
    /**
     * Returns the content ID (1-128 printable ASCII) to report for [source]. Return null or an empty string to keep
     * the connector's own: the asset IDs of a Google DAI source, else the one Lens derives from the source URL.
     */
    fun contentId(source: SourceDescription): String?
}
