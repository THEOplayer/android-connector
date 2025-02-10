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
    SKIPPING_TO_AD_BREAK,
    SKIPPED_TO_AD_BREAK,
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
    companion object {
        private const val TAG = "UplynkAdIntegration"
    }

    private var pingScheduler: PingScheduler? = null
    private var adScheduler: UplynkAdScheduler? = null
    private val player: Player
        get() = theoplayerView.player

    private var seekToTime: Duration = initSeekToTime()
    private var state = State.PLAYING_CONTENT

    init {
        player.addEventListener(PlayerEventTypes.TIMEUPDATE) {
            val time = timeDurationSeconds(it.currentTime)
            adScheduler?.onTimeUpdate(time)
            pingScheduler?.onTimeUpdate(time)

            when (state) {
                State.PLAYING_CONTENT,
                State.SKIPPING_TO_AD_BREAK, State.FINISHED_PLAYING_SKIPPED_AD_BREAK -> {
                    // Nothing to do
                }

                State.SKIPPED_TO_AD_BREAK -> {
                    // Sometimes time is slightly less that the ad break offset after seek, so wait for next TIMEUPDATE
                    state = State.PLAYING_SKIPPED_AD_BREAK
                }

                State.PLAYING_SKIPPED_AD_BREAK -> {
                    if (adScheduler?.isPlayingAd() == false) {
                        handleEndOfAdBreak()
                    }
                }
            }
        }

        player.addEventListener(PlayerEventTypes.SEEKING) {
            if (state == State.PLAYING_CONTENT) {
                pingScheduler?.onSeeking(timeDurationSeconds(it.currentTime))
            }
        }

        player.addEventListener(PlayerEventTypes.SEEKED) {
            handleSeeked(timeDurationSeconds(it.currentTime))
        }

        player.addEventListener(PlayerEventTypes.PLAY) {
            pingScheduler?.onStart(timeDurationSeconds(it.currentTime))
        }
    }

    private fun initSeekToTime() = 0.0.toDuration(DurationUnit.SECONDS)

    private fun handleEndOfAdBreak() {
        when (uplynkConfiguration.onSeekOverAd) {
            SkippedAdStrategy.PLAY_NONE -> {
                // Shouldn't come here
                Log.w(
                    TAG,
                    "Invalid state $state for SkippedAdStrategy ${SkippedAdStrategy.PLAY_NONE}"
                )
            }

            SkippedAdStrategy.PLAY_ALL -> {
                // If there are more ad breaks, play them else go to content
                adScheduler?.getUnWatchedAdBreakOffset(seekToTime)
                    ?.let { startOffset ->
                        snapback(startOffset)
                    } ?: goBackToContent()
            }

            SkippedAdStrategy.PLAY_LAST -> {
                goBackToContent()
            }
        }
    }

    private fun handleSeeked(time: Duration) {
        when (state) {
            State.PLAYING_CONTENT -> {
                when (uplynkConfiguration.onSeekOverAd) {
                    SkippedAdStrategy.PLAY_NONE -> {
                        // If the seek point is on an Ad break, jump to the start of the Ad break
                        seekToTime = adScheduler?.getAdBreakOffset(time) ?: time

                        if (seekToTime != time) {
                            // Seek point was on an Ad break
                            state = State.FINISHED_PLAYING_SKIPPED_AD_BREAK
                            seek(seekToTime)
                        }
                    }

                    SkippedAdStrategy.PLAY_ALL -> {
                        // If the seek point is on an Ad break, jump to the end of the Ad break after playing all ads
                        seekToTime = adScheduler?.getAdBreakEndTime(time) ?: time

                        adScheduler?.getUnWatchedAdBreakOffset(time)?.let { startOffset ->
                            snapback(startOffset)
                        }
                    }

                    SkippedAdStrategy.PLAY_LAST -> {
                        // If the seek point is on an Ad break, jump to the end of the Ad break after playing last ad
                        seekToTime = adScheduler?.getAdBreakEndTime(time) ?: time

                        adScheduler?.getLastUnWatchedAdBreakOffset(time)?.let { startOffset ->
                            snapback(startOffset)
                        }
                    }
                }
            }

            State.SKIPPING_TO_AD_BREAK -> {
                state = State.SKIPPED_TO_AD_BREAK
            }

            State.FINISHED_PLAYING_SKIPPED_AD_BREAK -> {
                state = State.PLAYING_CONTENT
                pingScheduler?.onSeeked(time)
            }

            State.SKIPPED_TO_AD_BREAK, State.PLAYING_SKIPPED_AD_BREAK -> {
                // Nothing to do
            }
        }
    }

    private fun seek(seekTime: Duration) {
        player.currentTime = seekTime.toDouble(DurationUnit.SECONDS)
    }

    private fun goBackToContent() {
        state = State.FINISHED_PLAYING_SKIPPED_AD_BREAK
        seek(seekToTime)
        seekToTime = initSeekToTime()
    }

    private fun snapback(startOffset: Duration) {
        // To handle TIMEUPDATE fired with old time before this seek completes
        state = State.SKIPPING_TO_AD_BREAK
        seek(startOffset)
    }

    override suspend fun resetSource() {
        adScheduler = null
        pingScheduler?.destroy()
    }

    override suspend fun setSource(source: SourceDescription): SourceDescription {
        adScheduler = null
        pingScheduler?.destroy()
        seekToTime = initSeekToTime()

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
                    adScheduler = UplynkAdScheduler(
                        listOf(),
                        AdHandler(controller, uplynkConfiguration.defaultSkipOffset)
                    )
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
                    adScheduler = UplynkAdScheduler(
                        response.ads.breaks,
                        AdHandler(controller, uplynkConfiguration.defaultSkipOffset)
                    )
                } catch (e: Exception) {
                    eventDispatcher.dispatchPreplayFailure(e)
                    controller.error(e)
                }
            }
    }

    override fun skipAd(ad: Ad) {
        adScheduler?.getSkipToTime(
            ad,
            player.currentTimeDurationSeconds(),
            uplynkConfiguration.defaultSkipOffset.toDuration(DurationUnit.SECONDS)
        )?.let {
            seek(it)
            adScheduler?.skipAd(ad)
        }
    }
}

fun timeDurationSeconds(time: Double) = time.toDuration(DurationUnit.SECONDS)

fun Player.currentTimeDurationSeconds() = timeDurationSeconds(this.currentTime)
