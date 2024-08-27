package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.AdBreakInit
import com.theoplayer.android.api.ads.AdInit
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.connector.uplynk.network.UplynkAd
import com.theoplayer.android.connector.uplynk.network.UplynkAdBreak
import java.util.WeakHashMap

private val Float.secToMs: Int
    get() = (this * 1000).toInt()

class AdHandler(private val controller: ServerSideAdIntegrationController) {
    private val scheduledAds = WeakHashMap<UplynkAd, Ad>()

    fun createAdBreak(adBreak: UplynkAdBreak) {
        val adBreakInit = AdBreakInit(adBreak.timeOffset.secToMs, adBreak.duration.secToMs)
        val currentAdBreak = controller.createAdBreak(adBreakInit)
        adBreak.ads.fold(adBreakInit.timeOffset) { offset, item ->
            scheduledAds[item] = controller.createAd(
                AdInit(
                    type = "linear",
                    timeOffset = offset,
                    duration = item.duration.secToMs
                ), currentAdBreak
            )
            offset + item.duration.secToMs
        }
    }

    fun onAdBegin(uplynkAd: UplynkAd) {
        val ad = scheduledAds[uplynkAd]
        check(ad != null) { "Cannot find an ad $uplynkAd" }
        controller.beginAd(ad)
    }

    fun onAdEnd(uplynkAd: UplynkAd) {
        val ad = scheduledAds[uplynkAd]
        check(ad != null) { "Cannot find an ad $uplynkAd" }
        controller.endAd(ad)
    }

    fun onAdProgressUpdate(currentAd: UplynkAdState, adBreak: UplynkAdBreak, time: Double) {
        val ad = scheduledAds[currentAd.ad]
        check(ad != null) { "Cannot find an ad: $currentAd" }

        val playedDuration = adBreak.ads
            .takeWhile { it != currentAd.ad }
            .sumOf { it.duration.toDouble() }

        val startTime = adBreak.timeOffset + playedDuration
        val progress = (time - startTime) / currentAd.ad.duration

        controller.updateAdProgress(ad, progress)
    }

}
