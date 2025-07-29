package com.theoplayer.android.connector.analytics.adscript

import android.app.Activity
import com.nad.adscriptapiclient.AdScriptDataObject
import com.nad.adscriptapiclient.AdScriptI12n
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.Ad

interface AdProcessor {
    fun apply(input: Ad): AdScriptDataObject
}

class AdscriptConnector(
    activity: Activity,
    playerView: THEOplayerView,
    configuration: AdscriptConfiguration,
    contentMetadata: AdScriptDataObject,
    adProcessor: AdProcessor?
) {
    private val adscriptAdapter = AdscriptAdapter(activity, configuration, playerView, contentMetadata, adProcessor)

    init {
        adscriptAdapter.start()
    }

    fun updateMetadata(metadata: AdScriptDataObject) {
        adscriptAdapter.update(metadata)
    }

    fun sessionStart() {
        adscriptAdapter.start()
    }

    fun updateUser(i12n: AdScriptI12n) {
        adscriptAdapter.updateUser(i12n)
    }
}