package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreakInit
import com.theoplayer.android.api.ads.AdInit
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.connector.uplynk.network.UplynkAd
import com.theoplayer.android.connector.uplynk.network.UplynkAdBreak
import java.util.WeakHashMap
import kotlin.time.Duration
import kotlin.time.DurationUnit

private val Duration.secToMs: Int
    get() = this.toInt(DurationUnit.MILLISECONDS)

@Suppress("UnstableApiUsage")
internal class AdHandler(private val controller: ServerSideAdIntegrationController) {
    private val scheduledAds = WeakHashMap<UplynkAd, Ad>()

    fun createAdBreak(adBreak: UplynkAdBreak) {
        val adBreakInit = AdBreakInit(adBreak.timeOffset.secToMs, adBreak.duration.secToMs)
        val currentAdBreak = controller.createAdBreak(adBreakInit)
        adBreak.ads.forEach {
            val adInit = AdInit(type = adBreak.type, duration = it.duration.secToMs)
            scheduledAds[it] = controller.createAd(adInit, currentAdBreak)
        }
    }

    fun onAdBegin(uplynkAd: UplynkAd) {
        val ad = scheduledAds[uplynkAd]
        checkNotNull(ad) { "Cannot find an ad $uplynkAd" }
        controller.beginAd(ad)
    }

    fun onAdEnd(uplynkAd: UplynkAd) {
        val ad = scheduledAds[uplynkAd]
        checkNotNull(ad) { "Cannot find an ad $uplynkAd" }
        controller.endAd(ad)
    }

    fun onAdProgressUpdate(currentAd: UplynkAdState, adBreak: UplynkAdBreak, time: Duration) {
        val ad = scheduledAds[currentAd.ad]
        checkNotNull(ad) { "Cannot find an ad: $currentAd" }

        val playedDuration = adBreak.ads
            .takeWhile { it != currentAd.ad }
            .fold(Duration.ZERO) { sum, item ->
                sum + item.duration
            }

        val startTime = adBreak.timeOffset + playedDuration
        val progress = ((time - startTime) / currentAd.ad.duration).coerceIn(0.0, 1.0)

        controller.updateAdProgress(ad, progress)
    }

}
