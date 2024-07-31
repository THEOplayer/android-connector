package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import kotlinx.serialization.Serializable

/**
 * The configuration for server-side ad insertion using the [UplynkConnector].
 */
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
        /**
         * Sets the prefix to use for Uplynk Media Platform Preplay API and Asset Info API requests.
         *
         * <ul>
         *     <li>If no prefix is set the default origin is used: https://content.uplynk.com
         * </ul>
         *
         * @param prefix The origin prefix to be used for Uplynk Media Platform requests.
         */
        fun prefix(prefix: String) = apply { this.prefix = prefix }

        private var assetIds = emptyList<String>()
        /**
         * Sets a list of asset IDs for Uplynk Media Platform Preplay API.
         *
         * @param externalId An external identifier. (<b>NonNull</b>)
         */
        fun assetIds(ids: List<String>) = apply { this.assetIds = ids }

        private var externalIds: List<String> = emptyList<String>()
        /**
         * Sets a list of external IDs for Uplynk Media Platform Preplay API.
         * If [assetIds] have at least one value this property is ignored and could be empty
         *
         * @param externalIds External identifiers. (<b>NonNull</b>)
         */
        fun externalIds(ids: List<String>) = apply { this.externalIds = ids }

        private var userId: String? = null
        /**
         * Sets a User ID for Uplynk Media Platform Preplay API.
         * If [assetIds] have at least one value this property is ignored and could be empty
         *
         * @param externalId An external identifier. (<b>NonNull</b>)
         */
        fun userId(id: String) = apply { this.userId = id }

        private var preplayParameters: LinkedHashMap<String, String> = LinkedHashMap()
        /**
         * Sets the parameters.
         *
         * <ul>
         *     <li>Each entry of the map contains the parameter name with associated value.
         *     <li>The parameters keep their order as it's maintained by LinkedHashMap.
         * </ul>
         *
         * @param parameters The parameters set for the Uplynk Media Platform API configuration.
         * <p>Example:</p>
         * <code>{ "ad": "exampleAdServer" }</code>
         *
         */
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
