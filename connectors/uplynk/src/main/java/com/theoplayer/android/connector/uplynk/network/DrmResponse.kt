package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

/**
 * The Uplynk content protection information.
 *
 * For further details, please refer to the Uplynk Documentation:
 * [Preplay API (Version 2) Documentation](https://docs.edgecast.com/video/#Develop/Preplayv2.htm)
 */
@Serializable
data class DrmResponse(
    /**
     * Indicates whether DRM is required for playback.
     */
    val required: Boolean = false,
    /**
     * The Fairplay certificate URL. (**Nullable**)
     */
    val fairplayCertificateURL: String? = null,

    /**
     * The Widevine license URL. (**Nullable**)
     */
    val widevineLicenseURL: String? = null,

    /**
     * The PlayReady license URL. (**Nullable**)
     */
    val playreadyLicenseURL: String? = null
)
