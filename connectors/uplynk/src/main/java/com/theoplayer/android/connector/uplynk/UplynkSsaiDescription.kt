package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import kotlinx.serialization.Serializable

@Serializable
class UplynkSsaiDescription(
    val prefix: String?,
    val assetIds: List<String>,
    val externalId: List<String>,
    val userId: String?,
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
        fun assetIds(ids: List<String>) = apply { this.assetIds = ids }

        private var externalIds: List<String> = emptyList<String>()
        fun externalIds(ids: List<String>) = apply { this.externalIds = ids }

        private var userId: String? = null
        fun userId(id: String) = apply { this.userId = id }


        private var preplayParameters: LinkedHashMap<String, String> = LinkedHashMap()
        fun preplayParameters(parameters: LinkedHashMap<String, String>) = apply { this.preplayParameters = parameters }

        /**
         * Builds the [UplynkSsaiDescription].
         */
        fun build() = UplynkSsaiDescription(
            prefix = prefix,
            assetIds = assetIds,
            externalId = externalIds,
            userId = userId,
            preplayParameters = preplayParameters)
    }
}
