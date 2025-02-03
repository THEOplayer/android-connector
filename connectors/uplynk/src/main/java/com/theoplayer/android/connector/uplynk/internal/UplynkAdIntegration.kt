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
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

enum class State {
    PLAYING_CONTENT,
    PLAYING_SKIPPED_AD_BREAK,
    FINISHED_PLAYING_SKIPPED_AD_BREAK,
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

    init {
        player.addEventListener(PlayerEventTypes.TIMEUPDATE) {
            val time = it.currentTime.toDuration(DurationUnit.SECONDS)
            Log.d("AdScheduler", "TIMEUPDATE currentTime ${it.currentTime} time $time")
            adScheduler?.onTimeUpdate(time)
            pingScheduler?.onTimeUpdate(time)

            if (state == State.PLAYING_SKIPPED_AD_BREAK) {
                if (adScheduler?.isPlayingAd() == false) {
                    Log.d("AdScheduler", "isPlayingAd false $time")
                    when (uplynkConfiguration.onSeekOverAd) {
                        SkippedAdStrategy.PLAY_NONE -> {}
                        SkippedAdStrategy.PLAY_ALL -> {
                            adScheduler?.getUnWatchedAdBreakOffset(seekTo!!)?.let { startOffset ->
                                snapback(startOffset)
                            } ?: goBackToContent()
                        }

                        SkippedAdStrategy.PLAY_LAST -> {
                            goBackToContent()
                        }
                    }
                }
            }
        }

        player.addEventListener(PlayerEventTypes.SEEKING) {
            // TODO handle backward seek

            val time = it.currentTime.toDuration(DurationUnit.SECONDS)

            if (state == State.PLAYING_CONTENT) {
                seekTo = time
                when (uplynkConfiguration.onSeekOverAd) {
                    SkippedAdStrategy.PLAY_NONE -> {
                        // TODO if seeked on to an ad
                    }

                    SkippedAdStrategy.PLAY_ALL -> {
                        adScheduler?.getUnWatchedAdBreakOffset(time)?.let { startOffset ->
                            snapback(startOffset)
                        }
                    }

                    SkippedAdStrategy.PLAY_LAST -> {
                        adScheduler?.getLastUnWatchedAdBreakOffset(time)?.let { startOffset ->
                            snapback(startOffset)
                        }
                    }
                }
            } else if (state == State.FINISHED_PLAYING_SKIPPED_AD_BREAK) {
                pingScheduler?.onSeeking(time)
            }
        }

        player.addEventListener(PlayerEventTypes.SEEKED) {
            val time = it.currentTime.toDuration(DurationUnit.SECONDS)

            if (state == State.FINISHED_PLAYING_SKIPPED_AD_BREAK) {
                state = State.PLAYING_CONTENT
                seekTo = null
                pingScheduler?.onSeeked(time)
            }
        }

        player.addEventListener(PlayerEventTypes.PLAY) {
            pingScheduler?.onStart(it.currentTime.toDuration(DurationUnit.SECONDS))
        }
    }

    private fun goBackToContent() {
        state = State.FINISHED_PLAYING_SKIPPED_AD_BREAK
        val actualSeekTo =
            checkNotNull(seekTo) { "Seek value cannot be null" } // TODO throw exception or silently fail to seek??
        Log.d("AdScheduler", "Going back to content $actualSeekTo")
        this.player.currentTime = actualSeekTo.toDouble(DurationUnit.SECONDS)
    }

    private fun snapback(startOffset: Duration) {
        Log.d("AdScheduler", "seeking to $startOffset")
        state = State.PLAYING_SKIPPED_AD_BREAK
        this.player.currentTime = startOffset.toDouble(DurationUnit.SECONDS)
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
