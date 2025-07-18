package com.theoplayer.android.connector.analytics.gemius

import com.gemius.sdk.stream.AdData
import com.gemius.sdk.stream.ProgramData
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.Ad

interface AdProcessor {
    fun apply(input: Ad): AdData
}

class GemiusConnector(
    configuration: GemiusConfiguration,
    playerView: THEOplayerView,
) {
    private val gemiusAdapter = GemiusAdapter(configuration,playerView, configuration.adProcessor)

    fun update(programId: String, programData: ProgramData) {
        gemiusAdapter.update(programId, programData)
    }
}