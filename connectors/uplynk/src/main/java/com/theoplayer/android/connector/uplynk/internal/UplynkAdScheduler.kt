package com.theoplayer.android.connector.uplynk.internal

import android.util.Log
import com.theoplayer.android.connector.uplynk.network.UplynkAd
import com.theoplayer.android.connector.uplynk.network.UplynkAdBreak
import com.theoplayer.android.connector.uplynk.network.UplynkAds
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration

data class UplynkAdBreakState(
    val adBreak: UplynkAdBreak,
    var state: AdBreakState,
    val ads: List<UplynkAdState> = adBreak.ads.map { UplynkAdState(it, AdState.NOT_PLAYED) }
)

data class UplynkAdState(
    val ad: UplynkAd,
    var state: AdState
)

enum class AdState {
    NOT_PLAYED,
    STARTED,
    COMPLETED,
}

enum class AdBreakState {
    NOT_PLAYED,
    STARTED,
    FINISHED,
}

internal class UplynkAdScheduler(
    uplynkAdBreaks: List<UplynkAdBreak>,
    private val adHandler: AdHandler
) {

    // TODO Test for empty
    private val adBreaks = CopyOnWriteArrayList(uplynkAdBreaks.map {
        adHandler.createAdBreak(it)
        UplynkAdBreakState(it, AdBreakState.NOT_PLAYED)
    })

    private fun moveToState(
        currentAdBreak: UplynkAdBreakState,
        newState: AdBreakState
    ) {
        if (currentAdBreak.state == newState) return
        currentAdBreak.state = newState
        if (currentAdBreak.state == AdBreakState.FINISHED) {
            endAllStartedAds(currentAdBreak)
        }
    }

    fun onTimeUpdate(time: Duration) {
        val currentAdBreak =
            adBreaks.firstOrNull { time in it.adBreak.timeOffset..(it.adBreak.timeOffset + it.adBreak.duration) }

        if (currentAdBreak != null) {
            val currentAd = beginCurrentAdBreak(currentAdBreak, time)
            endAllStartedAds(currentAdBreak, currentAd)
            beginCurrentAd(currentAdBreak, currentAd, time)
            endAllAdBreaksExcept(currentAdBreak)
        } else {
            endAllAdBreaks()
            Log.d("AdScheduler", "adBreak is null $time")
        }
    }

    private fun beginCurrentAd(
        currentAdBreak: UplynkAdBreakState,
        currentAd: UplynkAdState?,
        time: Duration
    ) {
        checkNotNull(currentAd) {
            "Current ad break exists but there is no current ad in $currentAdBreak"
        }
        when (currentAd.state) {
            AdState.COMPLETED,
            AdState.NOT_PLAYED -> {
                Log.d("AdScheduler", "beginCurrentAd $time")
                moveAdToState(currentAd, AdState.STARTED)
            }

            AdState.STARTED -> adHandler.onAdProgressUpdate(currentAd, currentAdBreak.adBreak, time)
        }
    }

    private fun moveAdToState(currentAd: UplynkAdState, state: AdState) {
        if (currentAd.state == state) return
        currentAd.state = state
        when (currentAd.state) {
            AdState.NOT_PLAYED -> {}
            AdState.STARTED -> adHandler.onAdBegin(currentAd.ad)
            AdState.COMPLETED -> adHandler.onAdEnd(currentAd.ad)
        }
    }

    private fun endAllStartedAds(
        currentAdBreak: UplynkAdBreakState,
        currentAd: UplynkAdState? = null
    ) = currentAdBreak.ads
        .filter { it.state == AdState.STARTED && it != currentAd }
        .forEach {
            moveAdToState(it, AdState.COMPLETED)
            Log.d(
                "AdScheduler",
                "endAllStartedAds ${currentAdBreak.adBreak.timeOffset} ${it.state}"
            )
        }

    private fun endAllAdBreaks() = adBreaks
        .filter { it.state == AdBreakState.STARTED }
        .forEach { moveToState(it, AdBreakState.FINISHED) }

    private fun endAllAdBreaksExcept(currentAdBreak: UplynkAdBreakState?) = adBreaks
        .filter { it.state == AdBreakState.STARTED && it != currentAdBreak }
        .forEach { moveToState(it, AdBreakState.FINISHED) }

    private fun beginCurrentAdBreak(
        currentAdBreak: UplynkAdBreakState,
        time: Duration
    ): UplynkAdState? {
        val currentAd = findCurrentAd(currentAdBreak, time)
        if (currentAdBreak.state != AdBreakState.STARTED) {
            moveToState(currentAdBreak, AdBreakState.STARTED)
            Log.d(
                "AdScheduler",
                "beginCurrentAdBreak time $time offset ${currentAdBreak.adBreak.timeOffset} ${currentAdBreak.state} size ${currentAdBreak.adBreak.ads.size} dur ${currentAdBreak.adBreak.duration}"
            )
        }
        return currentAd
    }

    private fun findCurrentAd(adBreak: UplynkAdBreakState, time: Duration): UplynkAdState? {
        var adStart = adBreak.adBreak.timeOffset
        for (ad in adBreak.ads) {
            val adEnd = adStart + ad.ad.duration
            if (time in adStart..adEnd) {
                return ad
            }
            adStart = adEnd
        }

        return null
    }

    fun add(ads: UplynkAds) = ads.breaks.forEach {
        adHandler.createAdBreak(it)
        adBreaks.add(UplynkAdBreakState(it, AdBreakState.NOT_PLAYED))
    }

    fun isPlayingAd(): Boolean {
        return adBreaks.firstOrNull { it.state == AdBreakState.STARTED } != null
    }

    fun skipAd(ad: UplynkAd) {
        adHandler.onAdSkip(ad)
    }

    fun getUnWatchedAdBreakOffset(time: Duration): Duration? {
        return adBreaks.firstOrNull { it.adBreak.timeOffset <= time && it.state == AdBreakState.NOT_PLAYED }?.adBreak?.timeOffset
    }

    fun getLastUnWatchedAdBreakOffset(time: Duration): Duration? {
        return adBreaks.lastOrNull { it.adBreak.timeOffset <= time && it.state == AdBreakState.NOT_PLAYED }?.adBreak?.timeOffset
    }

    fun getLastUnWatchedAdBreakEndTime(time: Duration): Duration? {
        return adBreaks.firstOrNull { (time in it.adBreak.timeOffset..(it.adBreak.timeOffset + it.adBreak.duration)) && it.state == AdBreakState.NOT_PLAYED }?.adBreak?.let {
            it.timeOffset + it.duration
        }
    }
}