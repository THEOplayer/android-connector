package com.theoplayer.android.connector.analytics.lens

import com.dolby.optiview.lens.LensBatch
import com.dolby.optiview.lens.LensConsent
import com.dolby.optiview.lens.LensDestination
import com.dolby.optiview.lens.LensJoinOptions

/**
 * The configuration of a [LensConnector]: the options of `Lens.join` (see [LensJoinOptions]), without the namespace and
 * the player, which the connector provides.
 *
 * @param destinations where to send the reports as well, besides the OptiView ingest.
 * @param consent the privacy gate, granted by default.
 * @param strict whether misuse throws instead of being dropped with a warning; for development builds.
 * @param heartbeatMs the cadence of the session's liveness report; null for Lens's default.
 * @param batch delivery hints for the OptiView ingest queue.
 * @param contentIdProvider returns the content ID to report for a source, overriding the one Lens derives from the
 * source URL, or the connector from a Google DAI source's asset IDs.
 */
data class LensConfiguration @JvmOverloads constructor(
    val destinations: List<LensDestination> = emptyList(),
    val consent: LensConsent = LensConsent.GRANTED,
    val strict: Boolean = false,
    val heartbeatMs: Long? = null,
    val batch: LensBatch? = null,
    val contentIdProvider: ContentIdProvider? = null,
)
