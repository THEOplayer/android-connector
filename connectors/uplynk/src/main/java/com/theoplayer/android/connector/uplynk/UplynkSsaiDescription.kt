package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import kotlinx.serialization.Serializable

/**
 * The configuration for server-side ad insertion using the [UplynkConnector].
 */
@Serializable
data class UplynkSsaiDescription(
    val prefix: String? = null,
    val assetIds: List<String> = listOf(),
    val externalId: List<String> = listOf(),
    val userId: String? = null,
    val contentProtected: Boolean = false,
    val preplayParameters: LinkedHashMap<String, String> = linkedMapOf(),
    val assetInfo: Boolean = false,
    val assetType: UplynkAssetType = UplynkAssetType.ASSET,
    val pingConfiguration: UplynkPingConfiguration = UplynkPingConfiguration()
) : CustomSsaiDescription() {

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
         * - If no prefix is set the default origin is used: https://content.uplynk.com
         *
         * @param prefix The origin prefix to be used for Uplynk Media Platform requests.
         */
        fun prefix(prefix: String) = apply { this.prefix = prefix }

        private var assetIds = emptyList<String>()

        /**
         * Sets a list of asset IDs for Uplynk Media Platform Preplay API.
         *
         * @param ids List of assets identifiers. (<b>NonNull</b>)
         */
        fun assetIds(ids: List<String>) = apply { this.assetIds = ids }

        private var externalIds: List<String> = emptyList<String>()

        /**
         * Sets a list of external IDs for Uplynk Media Platform Preplay API.
         * If [assetIds] have at least one value this property is ignored and could be empty
         *
         * @param ids External identifiers. (<b>NonNull</b>)
         */
        fun externalIds(ids: List<String>) = apply { this.externalIds = ids }

        private var userId: String? = null

        /**
         * Sets a User ID for Uplynk Media Platform Preplay API.
         * If [assetIds] have at least one value this property is ignored and could be empty
         *
         * @param id A user identifier. (<b>NonNull</b>)
         */
        fun userId(id: String) = apply { this.userId = id }

        private var contentProtected: Boolean = false
        /**
         * Sets whether the assets of the source are content protected.
         *
         * @param contentProtected Whether the assets of the source are content protected.
         */
        fun contentProtected(contentProtected: Boolean) = apply { this.contentProtected = contentProtected }

        private var preplayParameters: LinkedHashMap<String, String> = LinkedHashMap()

        /**
         * Sets the parameters.
         *
         * - Each entry of the map contains the parameter name with associated value.
         * - The parameters keep their order as it's maintained by LinkedHashMap.
         *
         * @param parameters The parameters set for the Uplynk Media Platform API configuration.
         * Example:
         * ```
         * linkedMapOf("ad" to "exampleAdServer")
         * ```
         */
        fun preplayParameters(parameters: LinkedHashMap<String, String>) =
            apply { this.preplayParameters = parameters }

        private var assetInfo: Boolean = false

        /**
         * Sets flag to request asset info.
         */
        fun assetInfo(shouldRequest: Boolean) = apply { this.assetInfo = shouldRequest }

        private var assetType: UplynkAssetType = UplynkAssetType.ASSET

        /**
         * Sets the asset type.
         *
         *   - For all possibilities, see {@link UplynkAssetType}.
         *
         *   @param value The Verizon Media asset type. (<b>NonNull</b>)
         *
         */
        fun assetType(value: UplynkAssetType) = apply { this.assetType = value }

        private var pingConfiguration: UplynkPingConfiguration = UplynkPingConfiguration()

        fun pingConfiguration(value: UplynkPingConfiguration) = apply { this.pingConfiguration = value }

        /**
         * Builds the [UplynkSsaiDescription].
         */
        fun build() = UplynkSsaiDescription(
            prefix = prefix,
            assetIds = assetIds,
            externalId = externalIds,
            userId = userId,
            contentProtected = contentProtected,
            preplayParameters = preplayParameters,
            assetInfo = assetInfo,
            assetType = assetType,
            pingConfiguration = pingConfiguration
        )
    }
}

/**
 * Describes the configuration of Verizon Media Ping features.
 *
 */
@Serializable
data class UplynkPingConfiguration(
    /**
     * Whether to increase the accuracy of ad events by passing the current playback time in Ping requests.
     *
     * @remark Only available when {@link UplynkSsaiDescription.assetType} is `'asset'`.
     *
     * @defaultValue `false`
     *
     */
    val adImpressions: Boolean = false,

    /**
     * Whether to enable FreeWheel's Video View by Callback feature to send content impressions to the FreeWheel server.
     *
     * @remarks Only available when {@link UplynkSsaiDescription.assetType} is `'asset'`.
     *
     * @defaultValue `false`
     */
    val freeWheelVideoViews: Boolean = false,

    /**
     * Whether to request information about upcoming ad breaks in the Ping responses.
     *
     * @defaultValue false.
     */
    val linearAdData: Boolean = false) {
    class Builder {
        private var adImpressions: Boolean = false
        fun adImpressions(value: Boolean) = apply { adImpressions = value }
        private var freeWheelVideoViews: Boolean = false
        fun freeWheelVideoViews(value: Boolean) = apply { freeWheelVideoViews = value }
        private var linearAdData: Boolean = false
        fun linearAdData(value: Boolean) = apply { linearAdData = value }

        fun build() = UplynkPingConfiguration(
            adImpressions = adImpressions,
            freeWheelVideoViews = freeWheelVideoViews,
            linearAdData = linearAdData
        )
    }
}

enum class UplynkAssetType {
    /**
     * A Video-on-demand content asset.
     */
    ASSET,

    /**
     * A Live content channel.
     */
    CHANNEL,

    /**
     * A Live event.
     */
    EVENT;
}
