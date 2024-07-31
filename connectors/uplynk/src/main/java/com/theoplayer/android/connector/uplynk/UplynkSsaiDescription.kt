package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import kotlinx.serialization.Serializable

@Serializable
class UplynkSsaiDescription(
    val prefix: String?,
    val assetIds: List<String>,
    val preplayParameters: LinkedHashMap<String, String>
): CustomSsaiDescription() {

    override val customIntegration: String
        get() = UplynkConnector.INTEGRATION_ID

    /**
     * A builder for a [UplynkSsaiDescription].
     */
    class Builder {
        private var prefix: String? = null
        fun prefix(prefix: String) = apply { this.prefix = prefix }

        private var assetIds = emptyList<String>()
        fun assetIds(assetIds: List<String>) = apply { this.assetIds = assetIds }

        private var preplayParameters: LinkedHashMap<String, String> = LinkedHashMap()
        fun preplayParameters(parameters: LinkedHashMap<String, String>) = apply { this.preplayParameters = parameters }

        /**
         * Builds the [UplynkSsaiDescription].
         */
        fun build() = UplynkSsaiDescription(
            prefix = prefix,
            assetIds = assetIds,
            preplayParameters = preplayParameters)
    }
}
