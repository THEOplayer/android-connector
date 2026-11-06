package com.theoplayer.android.connector.analytics.lens.internal

import android.os.Handler
import android.os.Looper
import com.dolby.optiview.lens.LensBufferedRanges
import com.dolby.optiview.lens.LensPlayerAdapter
import com.dolby.optiview.lens.LensPlayerFact
import com.dolby.optiview.lens.LensPlayerListener
import com.dolby.optiview.lens.LensPlayerProperties
import com.dolby.optiview.lens.LensPlayerSnapshot
import com.dolby.optiview.lens.LensPlayerSource
import com.dolby.optiview.lens.LensPreload
import com.dolby.optiview.lens.LensPresentationMode
import com.dolby.optiview.lens.LensRendition
import com.dolby.optiview.lens.LensTimeRange
import com.dolby.optiview.lens.LensTrack
import com.dolby.optiview.lens.LensTrackKind
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.error.THEOplayerException
import com.theoplayer.android.api.event.Event
import com.theoplayer.android.api.event.EventDispatcher
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.EventType
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.theolive.TheoLiveEventTypes
import com.theoplayer.android.api.event.track.mediatrack.audio.AudioTrackEventTypes
import com.theoplayer.android.api.event.track.mediatrack.audio.list.AudioTrackListEventTypes
import com.theoplayer.android.api.event.track.mediatrack.video.VideoTrackEventTypes
import com.theoplayer.android.api.event.track.mediatrack.video.list.VideoTrackListEventTypes
import com.theoplayer.android.api.event.track.texttrack.list.TextTrackListEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.PreloadType
import com.theoplayer.android.api.player.PresentationMode
import com.theoplayer.android.api.player.track.mediatrack.MediaTrack
import com.theoplayer.android.api.player.track.mediatrack.quality.Quality
import com.theoplayer.android.api.player.track.mediatrack.quality.VideoQuality
import com.theoplayer.android.api.player.track.texttrack.TextTrackMode
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.ssai.dai.GoogleDaiLiveConfiguration
import com.theoplayer.android.api.source.ssai.dai.GoogleDaiVodConfiguration
import com.theoplayer.android.api.timerange.TimeRanges
import java.util.concurrent.Executor

private val LOW_LATENCY_TYPES = setOf("application/vnd.theo.hesp+json", "theolive", "millicast")

private val TEXT_KINDS = setOf("subtitles", "captions")

private fun TimeRanges.toLens(): List<LensTimeRange> = (0 until length()).map { LensTimeRange(getStart(it), getEnd(it)) }

private fun Double.positive(): Double? = takeIf { it.isFinite() && it > 0 }

private fun Int.positive(): Int? = takeIf { it > 0 }

private fun String?.orNull(): String? = takeUnless { it.isNullOrEmpty() }

private fun isHls(source: TypedSource?): Boolean {
    val type = source?.type
    if (type != null) return type == SourceType.HLS || type == SourceType.HLSX
    return source?.src.orEmpty().substringBefore('?').substringBefore('#').endsWith(".m3u8", ignoreCase = true)
}

/** An SSAI source has no URL until the player resolves one, which is not public: name it by its asset IDs instead. */
private fun ssaiContentId(source: TypedSource): String? =
    when (val dai = source.ssai) {
        is GoogleDaiLiveConfiguration -> dai.assetKey.orNull()
        is GoogleDaiVodConfiguration -> if (dai.contentSourceID.isNotEmpty() && dai.videoID.isNotEmpty()) "${dai.contentSourceID}/${dai.videoID}" else null
        else -> null
    }

private fun errorFact(error: THEOplayerException, fatal: Boolean) =
    LensPlayerFact.Error(fatal, error.code.id.toString(), error.message?.replace(Regex("\\s+"), " ")?.trim().orNull())

/**
 * Reports a THEOplayer to Lens through its public API. THEOplayer runs on the main thread, and so does everything here.
 */
internal class THEOplayerLensAdapter(private val playerView: THEOplayerView) : LensPlayerAdapter {
    private val player: Player = playerView.player
    private val mainHandler = Handler(Looper.getMainLooper())

    override val playerName = "THEOplayer"
    override val playerVersion: String? = playerView.version
    override val executor = Executor { mainHandler.post(it) }

    private val listeners = mutableListOf<LensPlayerListener>()
    private val detachers = mutableListOf<() -> Unit>()
    private val trackDetachers = mutableListOf<() -> Unit>()
    private var destroyed = false

    /** The description of the last `sourcechange`; `player.source` stays null until the player has finished setting it. */
    private var description: SourceDescription? = null

    /** The source THEOplayer chose from the description; `Player.currentSource` is not in every supported version. */
    private var chosen: TypedSource? = null
    private var source: LensPlayerSource? = null
    private var presentationMode = PresentationMode.INLINE
    private var lastProperties = LensPlayerProperties()
    private var lastSnapshot = LensPlayerSnapshot(currentTime = 0.0, paused = true, ended = false)

    override fun subscribe(listener: LensPlayerListener): AutoCloseable {
        if (listeners.isEmpty() && !destroyed) attach()
        listeners += listener
        lastProperties = properties()
        listener.onFact(LensPlayerFact.Initial(source, lastProperties))
        return AutoCloseable {
            if (listeners.remove(listener) && listeners.isEmpty()) detach()
        }
    }

    override fun snapshot(): LensPlayerSnapshot {
        if (destroyed) return lastSnapshot
        try {
            val loaded = description != null
            val metrics = player.metrics
            val density = playerView.resources.displayMetrics.density
            lastSnapshot =
                LensPlayerSnapshot(
                    currentTime = player.currentTime,
                    paused = player.isPaused,
                    ended = player.isEnded,
                    playheadDateTime = if (loaded) player.currentProgramDateTime?.time else null,
                    duration = player.duration.takeIf { loaded && !it.isNaN() },
                    seekable = if (loaded) player.seekable.toLens() else null,
                    buffered = if (loaded) listOf(LensBufferedRanges(player.buffered.toLens())) else null,
                    measuredThroughput = metrics?.currentBandwidthEstimate?.positive()?.toLong(),
                    droppedFrames = if (loaded) metrics?.droppedVideoFrames else null,
                    viewWidth = (playerView.width / density).toInt().positive(),
                    viewHeight = (playerView.height / density).toInt().positive(),
                )
        } catch (_: Exception) {
            // The last snapshot stands in: the contract never lets snapshot() throw.
        }
        return lastSnapshot
    }

    private fun emit(fact: LensPlayerFact) {
        for (listener in listeners.toList()) listener.onFact(fact)
    }

    private fun <E : Event<*>, T : E> EventDispatcher<E>.listen(
        type: EventType<T>,
        into: MutableList<() -> Unit> = detachers,
        listener: EventListener<T>,
    ) {
        addEventListener(type, listener)
        into += { removeEventListener(type, listener) }
    }

    private fun currentTypedSource(): TypedSource? {
        val sources = description?.sources ?: return null
        return chosen?.takeIf { it in sources } ?: sources.firstOrNull()
    }

    private fun lensSource(): LensPlayerSource? {
        val current = currentTypedSource() ?: return null
        val url = current.src.orNull()
        val contentId = if (url == null) ssaiContentId(current) else null
        if (url == null && contentId == null) return null
        return LensPlayerSource(
            url = url,
            contentId = contentId,
            mimeType = current.type?.mimeType,
            autoplay = player.isAutoplay,
            preload =
                when (player.preload) {
                    PreloadType.NONE -> LensPreload.NONE
                    PreloadType.METADATA -> LensPreload.METADATA
                    PreloadType.AUTO -> LensPreload.AUTO
                    else -> null
                },
        )
    }

    private fun tracks(): List<LensTrack>? {
        val hls = isHls(currentTypedSource())
        val result = mutableListOf<LensTrack>()
        for (track in player.videoTracks) result += mediaTrack(track, LensTrackKind.VIDEO, hls)
        for (track in player.audioTracks) result += mediaTrack(track, LensTrackKind.AUDIO, hls)
        for (track in player.textTracks) {
            if (track.kind !in TEXT_KINDS) continue
            result +=
                LensTrack(
                    id = track.uid.toString(),
                    kind = LensTrackKind.TEXT,
                    selected = track.mode == TextTrackMode.SHOWING,
                    language = track.language.orNull(),
                    label = track.label.orNull(),
                )
        }
        return result.ifEmpty { null }
    }

    private fun <Q : Quality> mediaTrack(
        track: MediaTrack<Q>,
        kind: LensTrackKind,
        hls: Boolean,
    ): LensTrack {
        // HLS declares a bitrate on variants only: THEOplayer gives each audio quality its variant's BANDWIDTH.
        val ladder = if (kind == LensTrackKind.AUDIO && hls) null else track.qualities
        val renditions =
            ladder?.filter { it.bandwidth > 0 }?.map { quality ->
                val video = quality as? VideoQuality
                LensRendition(
                    id = quality.uid.toString(),
                    bitrate = quality.bandwidth,
                    // An HLS variant's BANDWIDTH counts every media type it plays with.
                    combinedBitrate = if (kind == LensTrackKind.VIDEO && hls) true else null,
                    codecs = quality.codecs.orNull(),
                    width = video?.width?.positive(),
                    height = video?.height?.positive(),
                    frameRate = video?.frameRate?.positive(),
                )
            }
        return LensTrack(
            id = track.uid.toString(),
            kind = kind,
            selected = track.isEnabled,
            language = track.language.orNull(),
            label = track.label.orNull(),
            renditions = renditions?.ifEmpty { null },
            renderedRenditionId = if (ladder != null) track.activeQuality?.uid?.toString() else null,
        )
    }

    private fun properties(): LensPlayerProperties {
        if (destroyed) return LensPlayerProperties()
        val loaded = description != null
        val duration = player.duration
        val live = if (loaded && !duration.isNaN()) duration.isInfinite() else null
        val current = currentTypedSource()
        val lowLatency = current?.lowLatency == true || current?.type?.mimeType?.lowercase() in LOW_LATENCY_TYPES
        val buffered = player.buffered
        return LensPlayerProperties(
            live = live,
            lowLatency = if (live == true && lowLatency) true else null,
            tracks = if (loaded) tracks() else null,
            targetBuffer = player.abr.targetBuffer.toDouble().positive(),
            hasMediaData = if (loaded) (0 until buffered.length()).any { buffered.getEnd(it) > buffered.getStart(it) } else null,
            playbackRate = player.playbackRate,
            muted = player.isMuted,
            volume = player.volume,
            presentationMode =
                when (presentationMode) {
                    PresentationMode.FULLSCREEN -> LensPresentationMode.FULLSCREEN
                    PresentationMode.PICTURE_IN_PICTURE -> LensPresentationMode.PIP
                    else -> LensPresentationMode.INLINE
                },
        )
    }

    /** Pushes the properties whole, as the contract asks, but only when one of them changed. */
    private fun change() {
        val next = properties()
        if (next != lastProperties) {
            lastProperties = next
            emit(LensPlayerFact.Change(next))
        }
    }

    private fun announce(continuation: Boolean) {
        source = lensSource()
        lastProperties = properties()
        emit(LensPlayerFact.Source(source, lastProperties, continuation))
    }

    private fun watchTracks() {
        for (detach in trackDetachers) detach()
        trackDetachers.clear()
        for (track in player.videoTracks) {
            track.listen(VideoTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT, trackDetachers) { change() }
            track.listen(VideoTrackEventTypes.CHANGE, trackDetachers) { change() }
        }
        for (track in player.audioTracks) {
            track.listen(AudioTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT, trackDetachers) { change() }
            track.listen(AudioTrackEventTypes.CHANGE, trackDetachers) { change() }
        }
    }

    private fun onTracksChanged() {
        watchTracks()
        change()
    }

    private fun attach() {
        description = player.source
        source = lensSource()
        presentationMode = playerView.presentationMode
        player.listen(PlayerEventTypes.PLAY) { emit(LensPlayerFact.Play) }
        player.listen(PlayerEventTypes.PLAYING) { emit(LensPlayerFact.Playing) }
        player.listen(PlayerEventTypes.PAUSE) { emit(LensPlayerFact.Pause) }
        player.listen(PlayerEventTypes.WAITING) { emit(LensPlayerFact.Waiting) }
        player.listen(PlayerEventTypes.SEEKING) { emit(LensPlayerFact.Seeking) }
        player.listen(PlayerEventTypes.SEEKED) { emit(LensPlayerFact.Seeked) }
        player.listen(PlayerEventTypes.ENDED) { emit(LensPlayerFact.Ended) }
        player.listen(PlayerEventTypes.ERROR) { emit(errorFact(it.errorObject, fatal = true)) }
        player.listen(PlayerEventTypes.SOURCECHANGE) {
            description = it.source
            announce(continuation = false)
        }
        player.listen(PlayerEventTypes.CURRENTSOURCECHANGE) {
            chosen = it.currentSource
            // `player.source` is null while a new source is being set, so this is another source of the same
            // description, as on a fallback: it keeps the viewing session.
            val next = it.currentSource?.src
            if (source != null && description != null && player.source == description && next != null && next != source?.url) {
                announce(continuation = true)
            }
        }
        player.listen(PlayerEventTypes.PRESENTATIONMODECHANGE) {
            presentationMode = it.presentationMode
            change()
        }
        player.listen(PlayerEventTypes.RATECHANGE) { change() }
        player.listen(PlayerEventTypes.VOLUMECHANGE) { change() }
        player.listen(PlayerEventTypes.DURATIONCHANGE) { change() }
        player.listen(PlayerEventTypes.PROGRESS) { change() }
        player.listen(PlayerEventTypes.DESTROY) { onDestroy() }
        player.videoTracks.let { list ->
            list.listen(VideoTrackListEventTypes.ADDTRACK) { onTracksChanged() }
            list.listen(VideoTrackListEventTypes.REMOVETRACK) { onTracksChanged() }
            list.listen(VideoTrackListEventTypes.TRACKLISTCHANGE) { onTracksChanged() }
        }
        player.audioTracks.let { list ->
            list.listen(AudioTrackListEventTypes.ADDTRACK) { onTracksChanged() }
            list.listen(AudioTrackListEventTypes.REMOVETRACK) { onTracksChanged() }
            list.listen(AudioTrackListEventTypes.TRACKLISTCHANGE) { onTracksChanged() }
        }
        player.textTracks.let { list ->
            list.listen(TextTrackListEventTypes.ADDTRACK) { change() }
            list.listen(TextTrackListEventTypes.REMOVETRACK) { change() }
            list.listen(TextTrackListEventTypes.TRACKLISTCHANGE) { change() }
        }
        player.theoLive?.listen(TheoLiveEventTypes.INTENTTOFALLBACK) { event ->
            event.reason?.let { emit(errorFact(it, fatal = false)) }
        }
        watchTracks()
    }

    private fun detach() {
        for (detach in detachers + trackDetachers) detach()
        detachers.clear()
        trackDetachers.clear()
    }

    private fun onDestroy() {
        if (source != null) {
            source = null
            emit(LensPlayerFact.Source(null, LensPlayerProperties()))
        }
        detach()
        destroyed = true
    }
}
