package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.drm.DRMConfiguration
import com.theoplayer.android.api.source.drm.FairPlayKeySystemConfiguration
import com.theoplayer.android.api.source.drm.KeySystemConfiguration
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import com.theoplayer.android.connector.uplynk.internal.network.UplynkApi
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class UplynkAdIntegration(
    private val theoplayerView: THEOplayerView,
    private val controller: ServerSideAdIntegrationController,
    private val eventDispatcher: UplynkEventDispatcher,
    private val uplynkDescriptionConverter: UplynkSsaiDescriptionConverter,
    private val uplynkApi: UplynkApi
) : ServerSideAdIntegrationHandler {
    private var adScheduler: UplynkAdScheduler? = null
    private val player: Player
        get() = theoplayerView.player

    init {
        player.addEventListener(PlayerEventTypes.TIMEUPDATE) {
            adScheduler?.onTimeUpdate(it.currentTime.toDuration(DurationUnit.SECONDS))
        }
    }

    override suspend fun resetSource() {
        adScheduler = null
    }

    override suspend fun setSource(source: SourceDescription): SourceDescription {
        adScheduler = null

        val uplynkSource = source.sources.singleOrNull { it.ssai is UplynkSsaiDescription }
        val ssaiDescription = uplynkSource?.ssai as? UplynkSsaiDescription ?: return source

        val preplayUrl = uplynkDescriptionConverter.buildPreplayUrl(ssaiDescription)
        val internalResponse = uplynkApi.preplay(preplayUrl)
        val minimalResponse = internalResponse.parseMinimalResponse()

        var newUplynkSource = uplynkSource.replaceSrc(minimalResponse.playURL)
        minimalResponse.drm?.let { drm ->
            val drmBuilder = DRMConfiguration.Builder()
            drm.widevineLicenseURL?.let { drmBuilder.widevine(KeySystemConfiguration.Builder(it).build()) }
            drm.playreadyLicenseURL?.let { drmBuilder.playready(KeySystemConfiguration.Builder(it).build()) }
            drm.fairplayCertificateURL?.let { drmBuilder.fairplay(FairPlayKeySystemConfiguration.Builder("", it).build()) }
            newUplynkSource = newUplynkSource.replaceDrm(drmBuilder.build())
        }

        val newSource = source.replaceSources(source.sources.toMutableList().apply {
            remove(uplynkSource)
            add(0, newUplynkSource)
        })

        try {
            val externalResponse = internalResponse.parseExternalResponse()
            eventDispatcher.dispatchPreplayEvents(externalResponse)
            adScheduler = UplynkAdScheduler(externalResponse.ads.breaks, AdHandler(controller))
        } catch (e: Exception) {
            eventDispatcher.dispatchPreplayFailure(e)
            controller.error(e)
        }

        if (ssaiDescription.assetInfo) {
            uplynkDescriptionConverter
                .buildAssetInfoUrls(ssaiDescription, minimalResponse.sid)
                .mapNotNull {
                    try {
                        uplynkApi.assetInfo(it)
                    } catch (e: Exception) {
                        eventDispatcher.dispatchAssetInfoFailure(e)
                        controller.error(e)
                        null
                    }
                }
                .forEach { eventDispatcher.dispatchAssetInfoEvents(it) }
        }

        return newSource
    }
}
