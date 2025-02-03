package com.theoplayer.android.connector.uplynk.internal

import android.util.Log
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.drm.DRMConfiguration
import com.theoplayer.android.api.source.drm.KeySystemConfiguration
import com.theoplayer.android.connector.uplynk.SkippedAdStrategy
import com.theoplayer.android.connector.uplynk.UplynkAssetType
import com.theoplayer.android.connector.uplynk.UplynkConfiguration
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import com.theoplayer.android.connector.uplynk.internal.network.PreplayInternalLiveResponse
import com.theoplayer.android.connector.uplynk.internal.network.PreplayInternalVodResponse
import com.theoplayer.android.connector.uplynk.internal.network.UplynkApi
import com.theoplayer.android.connector.uplynk.network.UplynkAdBreak
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

enum class State {
    PLAYING_CONTENT,
    PLAYING_SKIPPED_ADS
}

@Suppress("UnstableApiUsage")
internal class UplynkAdIntegration(
    private val theoplayerView: THEOplayerView,
    private val controller: ServerSideAdIntegrationController,
    private val eventDispatcher: UplynkEventDispatcher,
    private val uplynkDescriptionConverter: UplynkSsaiDescriptionConverter,
    private val uplynkApi: UplynkApi,
    private val uplynkConfiguration: UplynkConfiguration
) : ServerSideAdIntegrationHandler {
    private var pingScheduler: PingScheduler? = null
    private var adScheduler: UplynkAdScheduler? = null
    private val player: Player
        get() = theoplayerView.player

    private var seekTo: Duration? = null
    private var state = State.PLAYING_CONTENT
    private var unWatchedAdBreaks: MutableList<UplynkAdBreak>? = null

    init {
        player.addEventListener(PlayerEventTypes.TIMEUPDATE) {
            val time = it.currentTime.toDuration(DurationUnit.SECONDS)
            Log.d("Time check", "$time")
            adScheduler?.onTimeUpdate(time)
            pingScheduler?.onTimeUpdate(time)

            if (state == State.PLAYING_SKIPPED_ADS) {
                if (adScheduler?.isPlayingAd() == false) {
                    unWatchedAdBreaks?.removeFirstOrNull()
                    unWatchedAdBreaks?.firstOrNull()?.let { adBreak ->
                        this.player.currentTime =
                            adBreak.timeOffset.toDouble(DurationUnit.SECONDS)
                    } ?: run {
                        state = State.PLAYING_CONTENT
                        val actualSeekTo =
                            checkNotNull(seekTo) { "Seek value cannot be null" } // TODO throw exception or silently fail to seek
                        this.player.currentTime = actualSeekTo.toDouble(DurationUnit.SECONDS)
                    }
                }
            }
        }

        player.addEventListener(PlayerEventTypes.SEEKING) {
            Log.d("Time SEEKING", "${it.currentTime.toDuration(DurationUnit.SECONDS)}")
            val time = it.currentTime.toDuration(DurationUnit.SECONDS)

            if (state == State.PLAYING_CONTENT) {
                pingScheduler?.onSeeking(time) // TODO

                seekTo = time
                when (uplynkConfiguration.onSeekOverAd) {
                    SkippedAdStrategy.PLAY_NONE -> seeking(time)
                    SkippedAdStrategy.PLAY_ALL -> {
                        unWatchedAdBreaks = adScheduler?.getUnWatchedAdBreaks(time)?.toMutableList()
                        snapback(time)
                    }

                    SkippedAdStrategy.PLAY_LAST -> {
                        unWatchedAdBreaks =
                            adScheduler?.getLastUnWatchedAdBreak(time)?.toMutableList()
                        snapback(time)
                    }
                }
            }
        }

        player.addEventListener(PlayerEventTypes.SEEKED) {
            Log.d("Time SEEKED", "${it.currentTime.toDuration(DurationUnit.SECONDS)}")
            val time = it.currentTime.toDuration(DurationUnit.SECONDS)

            if (state == State.PLAYING_CONTENT) {
                pingScheduler?.onSeeked(time)
                seekTo = null
                unWatchedAdBreaks = null
            }
        }

        player.addEventListener(PlayerEventTypes.PLAY) {
            pingScheduler?.onStart(it.currentTime.toDuration(DurationUnit.SECONDS))
        }
    }

    private fun snapback(time: Duration) {
        if (!unWatchedAdBreaks.isNullOrEmpty()) {
            state = State.PLAYING_SKIPPED_ADS
            unWatchedAdBreaks?.first()?.timeOffset?.toDouble(DurationUnit.SECONDS)
                ?.let { seekTo ->
                    this.player.currentTime = seekTo
                }
        }
    }

    private fun seeking(time: Duration) {
        pingScheduler?.onSeeking(time)
    }

    override suspend fun resetSource() {
        adScheduler = null
        pingScheduler?.destroy()
    }

    override suspend fun setSource(source: SourceDescription): SourceDescription {
        adScheduler = null
        pingScheduler?.destroy()

        val uplynkSource = source.sources.singleOrNull { it.ssai is UplynkSsaiDescription }
        val ssaiDescription = uplynkSource?.ssai as? UplynkSsaiDescription ?: return source

        val minimalResponse = if (ssaiDescription.assetType == UplynkAssetType.ASSET) {
            requestVod(ssaiDescription).parseMinimalResponse()
        } else {
            requestLive(ssaiDescription).parseMinimalResponse()
        }

        var newUplynkSource = uplynkSource.replaceSrc(minimalResponse.playURL)

        minimalResponse.drm?.let { drm ->
            if (drm.required) {
                val drmBuilder = DRMConfiguration.Builder().apply {
                    drm.widevineLicenseURL?.let {
                        widevine(
                            KeySystemConfiguration.Builder(it).build()
                        )
                    }
                    drm.playreadyLicenseURL?.let {
                        playready(
                            KeySystemConfiguration.Builder(it).build()
                        )
                    }
                }
                newUplynkSource = newUplynkSource.replaceDrm(drmBuilder.build())
            }
        }

        val newSource = source.replaceSources(source.sources.toMutableList().apply {
            remove(uplynkSource)
            add(0, newUplynkSource)
        })

        if (UplynkPingFeatures.from(ssaiDescription) != UplynkPingFeatures.NO_PING) {
            pingScheduler = PingScheduler(
                uplynkApi,
                uplynkDescriptionConverter,
                minimalResponse.prefix,
                minimalResponse.sid,
                eventDispatcher,
                adScheduler!!
            )
        }

        if (ssaiDescription.assetInfo) {
            uplynkDescriptionConverter
                .buildAssetInfoUrls(ssaiDescription, minimalResponse.sid, minimalResponse.prefix)
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

    private suspend fun requestLive(ssaiDescription: UplynkSsaiDescription): PreplayInternalLiveResponse {
        return uplynkDescriptionConverter
            .buildPreplayLiveUrl(ssaiDescription)
            .let { uplynkApi.preplayLive(it) }
            .also {
                try {
                    val response = it.parseExternalResponse()
                    eventDispatcher.dispatchPreplayLiveEvents(response)
                    adScheduler = UplynkAdScheduler(listOf(), AdHandler(controller))
                } catch (e: Exception) {
                    eventDispatcher.dispatchPreplayFailure(e)
                    controller.error(e)
                }
            }
    }

    private suspend fun requestVod(ssaiDescription: UplynkSsaiDescription): PreplayInternalVodResponse {
        return uplynkDescriptionConverter
            .buildPreplayVodUrl(ssaiDescription)
            .let { uplynkApi.preplayVod(it) }
            .also {
                try {
                    val response = it.parseExternalResponse()
                    eventDispatcher.dispatchPreplayEvents(response)
                    adScheduler = UplynkAdScheduler(response.ads.breaks, AdHandler(controller))
                } catch (e: Exception) {
                    eventDispatcher.dispatchPreplayFailure(e)
                    controller.error(e)
                }
            }
    }

    override fun skipAd(ad: Ad) {
        adScheduler?.skipAd(ad)
    }
}
