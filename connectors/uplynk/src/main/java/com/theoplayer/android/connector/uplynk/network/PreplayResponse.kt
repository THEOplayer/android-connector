package com.theoplayer.android.connector.uplynk.network

import kotlinx.serialization.Serializable

@Serializable
data class PreplayResponse(
    val playURL: String,
    val sid: String)
