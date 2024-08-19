package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

@Serializable
data class UplynkBreakOffsets(
    val index: Int,
    val timeOffset: Double
)