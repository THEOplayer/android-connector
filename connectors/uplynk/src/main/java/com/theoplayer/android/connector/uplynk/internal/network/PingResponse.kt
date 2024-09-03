package com.theoplayer.android.connector.uplynk.internal.network

import com.theoplayer.android.connector.uplynk.network.BoundaryDetail
import com.theoplayer.android.connector.uplynk.network.UplynkAds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class PingResponse(
    @SerialName("next_time")
    @Serializable(with = DurationToSecDeserializer::class)
    val nextTime: Duration,
    val ads: UplynkAds? = null,
    val boundaries: List<BoundaryDetail>? = null,
    val extensions: List<String>? = null,
    val error: String? = null
)
