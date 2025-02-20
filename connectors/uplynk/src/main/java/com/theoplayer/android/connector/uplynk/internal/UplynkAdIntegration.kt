package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.api.event.player.PlayerEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.SeekedEvent
import com.theoplayer.android.api.event.player.TimeUpdateEvent
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private enum class State {
    PLAYING_CONTENT,
    SKIPPING_TO_AD_BREAK,
    SKIPPED_TO_AD_BREAK,
    PLAYING_SKIPPED_AD_BREAK,
    FINISHED_PLAYING_SKIPPED_AD_BREAK,
}

private class SeekTime {
    var seekFromTime: Duration = Duration.ZERO
    var seekToTime: Duration = Duration.ZERO

    fun isSeekFromSet() = seekFromTime > Duration.ZERO

    fun reset() {
        seekFromTime = Duration.ZERO
        seekToTime = Duration.ZERO
    }
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

    private var state = State.PLAYING_CONTENT
    private val seekTime = SeekTime()
    private var lastTimeUpdate: Duration = Duration.ZERO

    init {
        player.addEventListener(PlayerEventTypes.TIMEUPDATE) {
            val time = it.currentTime.seconds
            lastTimeUpdate = time
            adScheduler?.onTimeUpdate(time)
            pingScheduler?.onTimeUpdate(time)

            updateState(it, time)
        }

        player.addEventListener(PlayerEventTypes.SEEKING) {
            if (state == State.PLAYING_CONTENT && !seekTime.isSeekFromSet()) {
                seekTime.seekFromTime = lastTimeUpdate
            }
        }

        player.addEventListener(PlayerEventTypes.SEEKED) {
            handleSeeked(it, it.currentTime.seconds)
        }

        player.addEventListener(PlayerEventTypes.PLAY) {
            pingScheduler?.onStart(it.currentTime.seconds)
        }
    }

    private fun updateState(event: PlayerEvent<*>, time: Duration) {
        when (state) {
            State.PLAYING_CONTENT -> {
                if (event is SeekedEvent) {
                    // If the seek point is on an Ad break, jump to the end of the Ad break
                    seekTime.seekToTime = adScheduler?.getAdBreakEndTime(time) ?: time

                    when (uplynkConfiguration.onSeekOverAd) {
                        SkippedAdStrategy.PLAY_NONE -> {
                            if (seekTime.seekToTime != time) {
                                // Seek point was on an Ad break
                                playFromSeekToTime()
                            }
                        }

                        SkippedAdStrategy.PLAY_ALL -> {
                            adScheduler?.getUnWatchedAdBreakOffset(time)?.let { startOffset ->
                                snapback(startOffset)
                            } ?: playFromSeekToTime()
                        }

                        SkippedAdStrategy.PLAY_LAST -> {
                            adScheduler?.getLastUnwatchedAdBreakOffset(seekTime.seekFromTime, seekTime.seekToTime)?.let { startOffset ->
                                snapback(startOffset)
                            } ?: playFromSeekToTime()
                        }
                    }
                }
            }

            State.SKIPPING_TO_AD_BREAK -> {
                if (event is SeekedEvent) {
                    state = State.SKIPPED_TO_AD_BREAK
                }
            }

            State.SKIPPED_TO_AD_BREAK -> {
                if (event is TimeUpdateEvent) {
                    // Sometimes time is slightly less that the ad break offset after seek, so wait for next TIMEUPDATE
                    state = State.PLAYING_SKIPPED_AD_BREAK
                }
            }

            State.PLAYING_SKIPPED_AD_BREAK -> {
                if (event is TimeUpdateEvent) {
                    if (adScheduler?.isPlayingAd() == false) {
                        handleEndOfAdBreak()
                    }
                }
            }

            State.FINISHED_PLAYING_SKIPPED_AD_BREAK -> {
                if (event is SeekedEvent) {
                    state = State.PLAYING_CONTENT
                    seekTime.reset()
                    pingScheduler?.onSeeked(time)
                }
            }
        }
    }

    private fun handleEndOfAdBreak() {
        when (uplynkConfiguration.onSeekOverAd) {
            SkippedAdStrategy.PLAY_NONE -> {
                // Nothing to do
            }

            SkippedAdStrategy.PLAY_ALL -> {
                // If there are more ad breaks, play them else go to content
                adScheduler?.getUnWatchedAdBreakOffset(seekTime.seekToTime)
                    ?.let { startOffset ->
                        snapback(startOffset)
                    } ?: playFromSeekToTime()
            }

            SkippedAdStrategy.PLAY_LAST -> {
                playFromSeekToTime()
            }
        }
    }

    private fun handleSeeked(seekedEvent: SeekedEvent, time: Duration) {
        if (time <= seekTime.seekFromTime) {
            handleBackwardSeeked(time)
        } else {
            handleForwardSeeked(seekedEvent, time)
        }
    }

    private fun handleBackwardSeeked(time: Duration) {
        seekTime.seekToTime = adScheduler?.getAdBreakOffset(time) ?: time

        // If the seek point is on an Ad break, jump to the start of the Ad break
        if (seekTime.seekToTime != time) {
            playFromSeekToTime()
        }
        seekTime.reset()
    }

    private fun handleForwardSeeked(seekedEvent: SeekedEvent, time: Duration) {
        updateState(seekedEvent, time)
    }

    private fun seek(seekTime: Duration) {
        player.currentTime = seekTime.toDouble(DurationUnit.SECONDS)
    }

    private fun playFromSeekToTime() {
        state = State.FINISHED_PLAYING_SKIPPED_AD_BREAK
        seek(seekTime.seekToTime)
        pingScheduler?.onSeeking(seekTime.seekFromTime)
    }

    private fun snapback(startOffset: Duration) {
        // To handle TIMEUPDATE fired with old time before this seek completes
        state = State.SKIPPING_TO_AD_BREAK
        seek(startOffset)
    }

    override suspend fun resetSource() {
        adScheduler = null
        pingScheduler?.destroy()
        seekTime.reset()
    }

    override suspend fun setSource(source: SourceDescription): SourceDescription {
        resetSource()

        val uplynkSource = source.sources.singleOrNull { it.ssai is UplynkSsaiDescription }
        val ssaiDescription = uplynkSource?.ssai as? UplynkSsaiDescription ?: return source

        val minimalResponse = if (ssaiDescription.assetType == UplynkAssetType.ASSET) {
            requestVod(ssaiDescription).parseMinimalResponse()
        } else {
            requestLive(ssaiDescription).parseMinimalResponse()
        }

        val playUrl = uplynkDescriptionConverter.buildPlaybackUrl(minimalResponse.playURL, ssaiDescription)
        var newUplynkSource = uplynkSource.replaceSrc(playUrl)

        minimalResponse.drm?.let { drm ->
            if (drm.required) {
                val drmBuilder = DRMConfiguration.Builder().apply {
                    drm.widevineLicenseURL?.let { widevine(KeySystemConfiguration.Builder(it).build()) }
                    drm.playreadyLicenseURL?.let { playready(KeySystemConfiguration.Builder(it).build()) }
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
                    adScheduler = UplynkAdScheduler(listOf(), AdHandler(controller, uplynkConfiguration.defaultSkipOffset))
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
                    adScheduler = UplynkAdScheduler(response.ads.breaks, AdHandler(controller, uplynkConfiguration.defaultSkipOffset))
                } catch (e: Exception) {
                    eventDispatcher.dispatchPreplayFailure(e)
                    controller.error(e)
                }
            }
    }

    override fun skipAd(ad: Ad) {
        if (isAdSkippable(ad)) {
            adScheduler?.getSkipToTime(ad, player.currentTime.seconds)?.let {
                seek(it)
                adScheduler?.skipAd(ad)
            }
        }
    }

    private fun isAdSkippable(ad: Ad): Boolean = ad.skipOffset != -1
}
