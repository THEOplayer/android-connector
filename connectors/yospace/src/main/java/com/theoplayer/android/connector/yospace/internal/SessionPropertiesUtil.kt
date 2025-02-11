package com.theoplayer.android.connector.yospace.internal

import com.yospace.admanagement.Session
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

internal fun Session.SessionProperties.copy(
    requestTimeout: Int = this.requestTimeout,
    resourceTimeout: Int = this.resourceTimeout,
    userAgent: String = this.userAgent,
    proxyUserAgent: String = this.proxyUserAgent,
    keepProxyAlive: Boolean = this.keepProxyAlive,
    prefetchResources: Boolean = this.prefetchResources,
    fireHistoricalBeacons: Boolean = this.fireHistoricalBeacons,
    applyEncryptedTracking: Boolean = this.applyEncryptedTracking,
    excludedCategories: Int = this.excludedCategories,
    consecutiveBreakTolerance: Int = this.consecutiveBreakTolerance,
    token: UUID = this.token,
    customHttpHeaders: Map<String, String> = this.customHttpHeaders,
) = Session.SessionProperties().apply {
    this.requestTimeout = requestTimeout
    this.resourceTimeout = resourceTimeout
    this.userAgent = userAgent
    this.proxyUserAgent = proxyUserAgent
    this.keepProxyAlive = keepProxyAlive
    this.prefetchResources = prefetchResources
    this.fireHistoricalBeacons = fireHistoricalBeacons
    this.applyEncryptedTracking = applyEncryptedTracking
    this.excludeFromSuppression(excludedCategories)
    this.consecutiveBreakTolerance = consecutiveBreakTolerance
    this.token = token
    this.customHttpHeaders = customHttpHeaders
}

@Serializable
internal data class SerializedSessionProperties(
    val requestTimeout: Int,
    val resourceTimeout: Int,
    val userAgent: String,
    val proxyUserAgent: String,
    val keepProxyAlive: Boolean,
    val prefetchResources: Boolean,
    val fireHistoricalBeacons: Boolean,
    val applyEncryptedTracking: Boolean,
    val excludedCategories: Int,
    val consecutiveBreakTolerance: Int,
    val token: String,
    val customHttpHeaders: Map<String, String>,
)

internal fun Session.SessionProperties.serialize() = SerializedSessionProperties(
    requestTimeout = requestTimeout,
    resourceTimeout = resourceTimeout,
    userAgent = userAgent,
    proxyUserAgent = proxyUserAgent,
    keepProxyAlive = keepProxyAlive,
    prefetchResources = prefetchResources,
    fireHistoricalBeacons = fireHistoricalBeacons,
    applyEncryptedTracking = applyEncryptedTracking,
    excludedCategories = excludedCategories,
    consecutiveBreakTolerance = consecutiveBreakTolerance,
    token = token.toString(),
    customHttpHeaders = customHttpHeaders,
)

internal fun SerializedSessionProperties.deserialize(): Session.SessionProperties {
    val serialized = this
    return Session.SessionProperties().apply {
        requestTimeout = serialized.requestTimeout
        resourceTimeout = serialized.resourceTimeout
        userAgent = serialized.userAgent
        proxyUserAgent = serialized.proxyUserAgent
        keepProxyAlive = serialized.keepProxyAlive
        prefetchResources = serialized.prefetchResources
        fireHistoricalBeacons = serialized.fireHistoricalBeacons
        applyEncryptedTracking = serialized.applyEncryptedTracking
        excludeFromSuppression(serialized.excludedCategories)
        consecutiveBreakTolerance = serialized.consecutiveBreakTolerance
        token = UUID.fromString(serialized.token)
        customHttpHeaders = serialized.customHttpHeaders
    }
}

internal object YospaceSessionPropertiesKSerializer : KSerializer<Session.SessionProperties> {
    private val delegateSerializer = SerializedSessionProperties.serializer()

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor =
        SerialDescriptor("YospaceSessionProperties", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: Session.SessionProperties) {
        encoder.encodeSerializableValue(delegateSerializer, value.serialize())
    }

    override fun deserialize(decoder: Decoder): Session.SessionProperties {
        return decoder.decodeSerializableValue(delegateSerializer).deserialize()
    }
}
