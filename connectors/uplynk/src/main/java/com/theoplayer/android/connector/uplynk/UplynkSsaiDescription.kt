package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import kotlinx.serialization.Serializable

@Serializable
class UplynkSsaiDescription(val srcURL: String): CustomSsaiDescription() {
    override val customIntegration: String
        get() = UplynkConnector.INTEGRATION_ID
    /**
     * A builder for a [UplynkSsaiDescription].
     */
    class Builder(val srcURL: String = "") {
        /**
         * Builds the [UplynkSsaiDescription].
         */
        fun build() = UplynkSsaiDescription(srcURL = srcURL)
    }
}
