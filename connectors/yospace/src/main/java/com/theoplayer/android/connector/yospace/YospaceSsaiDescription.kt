package com.theoplayer.android.connector.yospace

import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import com.yospace.admanagement.Session

/**
 * The configuration for server-side ad insertion using the Yospace connector.
 */
class YospaceSsaiDescription(
    /**
     * The type of the requested stream.
     *
     * Default: [YospaceStreamType.LIVE]
     */
    val streamType: YospaceStreamType = YospaceStreamType.LIVE,
    /**
     * Custom properties to set when initializing the Yospace session.
     */
    val sessionProperties: Session.SessionProperties = Session.SessionProperties()
) : CustomSsaiDescription() {
    override val customIntegrationId: String
        get() = INTEGRATION_ID
}

/**
 * The type of the Yospace stream.
 */
enum class YospaceStreamType {
    /**
     * The stream is a live stream.
     */
    LIVE,

    /**
     * The stream is a live stream with a large DVR window.
     */
    LIVEPAUSE,

    /**
     * The stream is a Non-Linear Start-Over stream.
     */
    NONLINEAR,

    /**
     * The stream is a video-on-demand stream.
     */
    VOD,
}
