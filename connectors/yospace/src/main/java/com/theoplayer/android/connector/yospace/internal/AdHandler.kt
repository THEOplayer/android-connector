package com.theoplayer.android.connector.yospace.internal

import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.AdBreakInit
import com.theoplayer.android.api.ads.AdInit
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.player.Player
import com.yospace.admanagement.AnalyticEventObserver
import com.yospace.admanagement.Resource
import com.yospace.admanagement.Session
import com.yospace.admanagement.TrackingErrors
import java.util.WeakHashMap
import com.yospace.admanagement.AdBreak as YospaceAdBreak
import com.yospace.admanagement.Advert as YospaceAdvert

internal class AdHandler(
    private val player: Player,
    private val controller: ServerSideAdIntegrationController,
    private val playheadConverter: PlayheadConverter
) : AnalyticEventObserver {
    private val ads: WeakHashMap<YospaceAdvert, Ad> = WeakHashMap()
    private val adBreaks: WeakHashMap<YospaceAdBreak, AdBreak> = WeakHashMap()
    private var currentAd: Ad? = null
    private var currentYospaceAdvert: YospaceAdvert? = null
    private var currentAdBreak: AdBreak? = null

    private fun getOrCreateAdBreak(yospaceAdBreak: YospaceAdBreak): AdBreak {
        val adBreak = adBreaks.getOrPut(yospaceAdBreak) { controller.createAdBreak(getAdBreakInit(yospaceAdBreak)) }
        yospaceAdBreak.adverts.forEach { getOrCreateAd(it, adBreak) }
        return adBreak
    }

    private fun getAdBreakInit(yospaceAdBreak: YospaceAdBreak): AdBreakInit {
        return AdBreakInit(
            timeOffset = playheadConverter.fromPlayhead(yospaceAdBreak.start).toInt(),
            maxDuration = yospaceAdBreak.duration / 1000
        )
    }

    private fun getOrCreateAd(yospaceAdvert: YospaceAdvert, adBreak: AdBreak): Ad {
        var update = true
        val ad = ads.getOrPut(yospaceAdvert) {
            update = false
            controller.createAd(getAdInit(yospaceAdvert), adBreak)
        }
        if (update) {
            controller.updateAd(ad, getAdInit(yospaceAdvert))
        }
        return ad
    }

    private fun getAdInit(advert: YospaceAdvert): AdInit {
        val nonLinearCreative = if (advert.isNonLinear) advert.getNonLinearCreatives(Resource.ResourceType.STATIC).firstOrNull() else null
        return AdInit(
            type = if (advert.isNonLinear) "nonlinear" else "linear",
            skipOffset = if (advert.skipOffset < 0) -1 else (advert.skipOffset / 1000).toInt(),
            id = advert.identifier,
            duration = (advert.duration / 1000).toInt(),
            clickThrough = if (advert.isNonLinear) nonLinearCreative?.clickThroughUrl else advert.linearCreative?.clickThroughUrl,
            resourceURI = nonLinearCreative?.getResource(Resource.ResourceType.STATIC)?.stringData
        )
    }

    override fun onAdvertBreakStart(adBreak: YospaceAdBreak?, session: Session) {
        currentAdBreak = if (adBreak == null) {
            // During live playback, an ad break may be started without any information
            controller.createAdBreak(AdBreakInit(timeOffset = player.currentTime.toInt()))
        } else {
            getOrCreateAdBreak(adBreak)
        }
    }

    override fun onAdvertBreakEnd(session: Session) {
        currentAdBreak?.let {
            controller.removeAdBreak(it)
            currentAdBreak = null
        }
    }

    override fun onAdvertStart(advert: YospaceAdvert, session: Session) {
        val ad = getOrCreateAd(advert, currentAdBreak ?: return)
        currentAd = ad
        currentYospaceAdvert = advert
        controller.beginAd(ad)

        advert.linearCreative?.let {
            // val clickThroughUrl = it.clickThroughUrl
            // TODO Show linear clickthrough button
        }

        advert.getNonLinearCreatives(Resource.ResourceType.STATIC).forEach {
            // val clickThroughUrl = it.clickThroughUrl
            // TODO Show nonlinear clickthrough button
        }
    }

    override fun onAdvertEnd(session: Session) {
        currentAd?.let {
            controller.endAd(it)
            currentAd = null
            currentYospaceAdvert = null
        }
        // TODO Remove clickthrough buttons
    }

    override fun onEarlyReturn(adBreak: YospaceAdBreak, session: Session) {
        currentAd?.let {
            controller.skipAd(it)
            currentAd = null
            currentYospaceAdvert = null
        }
        onAdvertBreakEnd(session)
    }

    override fun onAnalyticUpdate(session: Session) {}

    override fun onSessionError(error: AnalyticEventObserver.SessionError, session: Session) {}

    override fun onTrackingEvent(type: String, session: Session) {}

    override fun onTrackingError(error: TrackingErrors.Error, session: Session) {}

    fun onTimeUpdate(playhead: Long) {
        val ad = this.currentAd ?: return
        val advert = this.currentYospaceAdvert ?: return
        val duration = advert.duration
        val remainingTime = advert.getRemainingTime(playhead)
        val progress = ((duration - remainingTime).toDouble() / duration.toDouble()).coerceIn(0.0, 1.0)
        controller.updateAdProgress(ad, progress)
    }

    fun destroy() {
        controller.removeAllAds()
    }
}