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
//TODO check if this makes sense. Currently it looks like by separation of this we could make easier customization for the customer, but should be checked before PR
by InternalUplynkAdIntegration(
    theoplayerView,
    controller,
    UplynkSsaiDescriptionConverter())