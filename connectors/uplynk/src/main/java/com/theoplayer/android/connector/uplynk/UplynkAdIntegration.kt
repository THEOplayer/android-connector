package com.theoplayer.android.connector.uplynk

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.connector.uplynk.internal.InternalUplynkAdIntegration
import com.theoplayer.android.connector.uplynk.internal.UplynkSsaiDescriptionConverter

class UplynkAdIntegration(
    private val theoplayerView: THEOplayerView,
    private val controller: ServerSideAdIntegrationController
) : ServerSideAdIntegrationHandler
by InternalUplynkAdIntegration(
    theoplayerView,
    controller,
    UplynkSsaiDescriptionConverter())