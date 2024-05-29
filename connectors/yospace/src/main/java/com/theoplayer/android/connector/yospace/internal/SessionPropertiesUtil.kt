package com.theoplayer.android.connector.yospace.internal

import com.yospace.admanagement.Session
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SerializedSessionProperties(
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

fun Session.SessionProperties.serialize() = SerializedSessionProperties(
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

fun SerializedSessionProperties.deserialize(): Session.SessionProperties {
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