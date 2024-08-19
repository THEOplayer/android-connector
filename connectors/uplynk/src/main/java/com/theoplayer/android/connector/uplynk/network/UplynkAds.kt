package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

@Serializable
data class UplynkAds(
    val breaks: List<UplynkAdBreaks>,
    val breakOffsets: List<UplynkBreakOffsets>,
    val placeholderOffsets: List<UplynkPlaceholderAds>
)
