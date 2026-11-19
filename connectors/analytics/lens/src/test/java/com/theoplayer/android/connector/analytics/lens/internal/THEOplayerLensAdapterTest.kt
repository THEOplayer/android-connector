package com.theoplayer.android.connector.analytics.lens.internal

import com.dolby.optiview.lens.LensPlayerFact
import com.dolby.optiview.lens.LensPlayerSource
import com.dolby.optiview.lens.LensPreload
import com.dolby.optiview.lens.LensPresentationMode
import com.dolby.optiview.lens.LensRendition
import com.dolby.optiview.lens.LensTrack
import com.dolby.optiview.lens.LensTrackKind
import com.theoplayer.android.api.error.ErrorCode
import com.theoplayer.android.api.error.THEOplayerException
import com.theoplayer.android.api.event.player.CurrentSourceChangeEvent
import com.theoplayer.android.api.event.player.ErrorEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PresentationModeChange
import com.theoplayer.android.api.event.track.mediatrack.video.list.VideoTrackListEventTypes
import com.theoplayer.android.api.player.PresentationMode
import com.theoplayer.android.api.player.track.mediatrack.quality.AudioQuality
import com.theoplayer.android.api.player.track.mediatrack.quality.VideoQuality
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.metadata.MetadataDescription
import com.theoplayer.android.api.source.ssai.dai.GoogleDaiLiveConfiguration
import com.theoplayer.android.api.source.ssai.dai.GoogleDaiVodConfiguration
import com.theoplayer.android.api.theolive.TheoLiveSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class THEOplayerLensAdapterTest {
    private val theoplayer = TestTHEOplayer()
    private val dispatchers = theoplayer.dispatchers
    private val player = theoplayer.player
    private val view = theoplayer.view
    private val facts = mutableListOf<LensPlayerFact>()
    private var adapter = THEOplayerLensAdapter(view)

    private fun subscribe(): AutoCloseable = adapter.subscribe { facts += it }

    private fun setSource(description: SourceDescription?) = theoplayer.setSource(description)

    @Test
    fun subscribe_reportsTheCurrentSourceFirst() {
        whenever(player.source).thenReturn(SourceDescription(listOf(TypedSource("https://cdn.example.com/a.m3u8", type = SourceType.HLS))))
        whenever(player.isAutoplay).thenReturn(true)

        subscribe()

        val initial = facts.single() as LensPlayerFact.Initial
        assertEquals(
            LensPlayerSource(url = "https://cdn.example.com/a.m3u8", mimeType = "application/vnd.apple.mpegurl", autoplay = true, preload = LensPreload.AUTO),
            initial.source,
        )
        assertEquals(LensPresentationMode.INLINE, initial.properties.presentationMode)
    }

    @Test
    fun target_isTheTHEOplayerPlayer() {
        assertSame(player, adapter.target)
    }

    @Test
    fun playerEvents_areReportedAsPlaybackFacts() {
        subscribe()

        dispatchers.fire(PlayerEventTypes.PLAY)
        dispatchers.fire(PlayerEventTypes.PLAYING)
        dispatchers.fire(PlayerEventTypes.PAUSE)
        dispatchers.fire(PlayerEventTypes.WAITING)
        dispatchers.fire(PlayerEventTypes.SEEKING)
        dispatchers.fire(PlayerEventTypes.SEEKED)
        dispatchers.fire(PlayerEventTypes.ENDED)

        assertEquals(
            listOf(
                LensPlayerFact.Play, LensPlayerFact.Playing, LensPlayerFact.Pause, LensPlayerFact.Waiting,
                LensPlayerFact.Seeking, LensPlayerFact.Seeked, LensPlayerFact.Ended,
            ),
            facts.drop(1),
        )
    }

    @Test
    fun sourceChange_reportsTheNewSource() {
        subscribe()

        setSource(SourceDescription(listOf(TypedSource("https://cdn.example.com/b.mpd", type = SourceType.DASH))))

        val source = facts.filterIsInstance<LensPlayerFact.Source>().single()
        assertEquals("https://cdn.example.com/b.mpd", source.source?.url)
        assertEquals(false, source.continuation)
    }

    @Test
    fun sourceChange_namesAGoogleDaiSourceByItsAssetIds() {
        subscribe()

        setSource(SourceDescription(listOf(TypedSource("", type = SourceType.HLS, ssai = GoogleDaiVodConfiguration.Builder("", "2474148", "bbb-clear").build()))))

        val source = facts.filterIsInstance<LensPlayerFact.Source>().single().source
        assertNull(source?.url)
        assertEquals("2474148/bbb-clear", source?.contentId)
    }

    @Test
    fun sourceChange_takesTheContentIdFromTheProviderFirst() {
        adapter = THEOplayerLensAdapter(view) { it.metadata?.get("title") as? String }
        subscribe()

        setSource(
            SourceDescription(
                listOf(TypedSource("", type = SourceType.HLS, ssai = GoogleDaiLiveConfiguration.Builder("", "asset-key").build())),
                metadata = MetadataDescription(mutableMapOf<String, Any?>("title" to "movie-1")),
            ),
        )

        assertEquals("movie-1", facts.filterIsInstance<LensPlayerFact.Source>().single().source?.contentId)

        setSource(
            SourceDescription(
                listOf(TypedSource("", type = SourceType.HLS, ssai = GoogleDaiLiveConfiguration.Builder("", "asset-key").build())),
                metadata = MetadataDescription(mutableMapOf<String, Any?>("title" to "")),
            ),
        )

        assertEquals("asset-key", facts.filterIsInstance<LensPlayerFact.Source>().last().source?.contentId)
    }

    @Test
    fun currentSourceChange_onAFallbackKeepsTheSession() {
        val primary = TypedSource("https://primary.example.com/a.m3u8")
        val description = SourceDescription(listOf(primary, TypedSource("https://backup.example.com/a.m3u8")))
        subscribe()
        setSource(description)
        facts.clear()

        dispatchers.fire(PlayerEventTypes.CURRENTSOURCECHANGE, mock<CurrentSourceChangeEvent> { on { currentSource } doReturn description.sources[1] })

        val source = facts.filterIsInstance<LensPlayerFact.Source>().single()
        assertEquals("https://backup.example.com/a.m3u8", source.source?.url)
        assertEquals(true, source.continuation)
    }

    @Test
    fun error_isReportedAsFatalWithItsCodeAndCauseOnOneLine() {
        subscribe()

        val error = THEOplayerException(ErrorCode.MANIFEST_LOAD_ERROR, "Failed to load\n  the manifest", Exception("HTTP 404"))
        dispatchers.fire(PlayerEventTypes.ERROR, mock<ErrorEvent> { on { errorObject } doReturn error })

        assertEquals(LensPlayerFact.Error(true, ErrorCode.MANIFEST_LOAD_ERROR.id.toString(), "Failed to load the manifest (HTTP 404)"), facts.last())
    }

    @Test
    fun eventsWhileANewSourceIsSet_noLongerBelongToTheOldSource() {
        whenever(player.source).thenReturn(SourceDescription(listOf(TypedSource("https://cdn.example.com/a.m3u8"))))
        subscribe()

        whenever(player.source).thenReturn(null)
        val error = THEOplayerException(ErrorCode.LICENSE_INVALID_SOURCE, "Invalid source")
        dispatchers.fire(PlayerEventTypes.ERROR, mock<ErrorEvent> { on { errorObject } doReturn error })

        assertNull((facts[1] as LensPlayerFact.Source).source)
        assertEquals(LensPlayerFact.Error(true, ErrorCode.LICENSE_INVALID_SOURCE.id.toString(), "Invalid source"), facts[2])
    }

    @Test
    fun playbackWithoutAPlayEvent_reportsThePlayRequestFirst() {
        subscribe()
        setSource(SourceDescription(listOf(TypedSource("https://cdn.example.com/a.mp4"))))
        facts.clear()

        whenever(player.isPaused).thenReturn(false)
        dispatchers.fire(PlayerEventTypes.PLAYING)
        dispatchers.fire(PlayerEventTypes.WAITING)

        assertEquals(listOf(LensPlayerFact.Play, LensPlayerFact.Playing, LensPlayerFact.Waiting), facts)
    }

    @Test
    fun propertyChanges_arePushedWholeOnlyWhenSomethingChanged() {
        subscribe()

        whenever(player.isMuted).thenReturn(true)
        dispatchers.fire(PlayerEventTypes.VOLUMECHANGE)
        dispatchers.fire(PlayerEventTypes.VOLUMECHANGE)
        whenever(view.presentationMode).thenReturn(PresentationMode.FULLSCREEN)
        dispatchers.fire(PlayerEventTypes.PRESENTATIONMODECHANGE, mock<PresentationModeChange> { on { presentationMode } doReturn PresentationMode.FULLSCREEN })

        val changes = facts.filterIsInstance<LensPlayerFact.Change>()
        assertEquals(2, changes.size)
        assertEquals(true, changes[1].properties.muted)
        assertEquals(LensPresentationMode.FULLSCREEN, changes[1].properties.presentationMode)
    }

    @Test
    fun lowLatency_followsTheSourceFlagOrItsType() {
        subscribe()
        val lowLatency = listOf(
            TypedSource("https://cdn.example.com/ll.m3u8", lowLatency = true),
            TypedSource("https://cdn.example.com/hesp.json", type = SourceType.HESP),
            TheoLiveSource(src = "channel-id"),
            TypedSource("https://cdn.example.com/a.m3u8"),
        ).map { typed ->
            setSource(SourceDescription(listOf(typed)))
            (facts.last() as LensPlayerFact.Source).properties.lowLatency
        }

        assertEquals(listOf(true, true, null, null), lowLatency)
    }

    @Test
    fun duration_tellsLiveFromVod() {
        subscribe()
        setSource(SourceDescription(listOf(TypedSource("https://cdn.example.com/a.m3u8"))))

        whenever(player.duration).thenReturn(Double.POSITIVE_INFINITY)
        dispatchers.fire(PlayerEventTypes.DURATIONCHANGE)
        val live = (facts.last() as LensPlayerFact.Change).properties.live
        whenever(player.duration).thenReturn(60.0)
        dispatchers.fire(PlayerEventTypes.DURATIONCHANGE)

        assertEquals(true to false, live to (facts.last() as LensPlayerFact.Change).properties.live)
    }

    @Test
    fun hlsTracks_carryCombinedVideoBitratesAndNoAudioLadder() {
        subscribe()
        setSource(SourceDescription(listOf(TypedSource("https://cdn.example.com/a.m3u8"))))
        val quality = mock<VideoQuality> {
            on { uid } doReturn 7
            on { bandwidth } doReturn 2_000_000L
            on { codecs } doReturn "avc1.64001f"
            on { width } doReturn 1280
            on { height } doReturn 720
        }
        val audioQuality = mock<AudioQuality> { on { bandwidth } doReturn 2_000_000L }
        val videoLadder = qualityList(quality)
        val audioLadder = qualityList(audioQuality)
        theoplayer.video = listOf(mock { on { uid } doReturn 1; on { isEnabled } doReturn true; on { qualities } doReturn videoLadder; on { activeQuality } doReturn quality })
        theoplayer.audio = listOf(mock { on { uid } doReturn 2; on { isEnabled } doReturn true; on { language } doReturn "en"; on { qualities } doReturn audioLadder })

        dispatchers.fire(VideoTrackListEventTypes.ADDTRACK)

        assertEquals(
            listOf(
                LensTrack(
                    id = "video-1", kind = LensTrackKind.VIDEO, selected = true,
                    renditions = listOf(LensRendition(id = "7", bitrate = 2_000_000L, combinedBitrate = true, codecs = "avc1.64001f", width = 1280, height = 720)),
                    renderedRenditionId = "7",
                ),
                LensTrack(id = "audio-2", kind = LensTrackKind.AUDIO, selected = true, language = "en"),
            ),
            (facts.last() as LensPlayerFact.Change).properties.tracks,
        )
    }

    @Test
    fun buffering_beforePlay_reportsMediaData() {
        subscribe()
        setSource(SourceDescription(listOf(TypedSource("https://cdn.example.com/a.mp4"))))

        doReturn(timeRanges(0.0 to 4.0)).whenever(player).buffered
        dispatchers.fire(PlayerEventTypes.PROGRESS)

        assertEquals(true, (facts.last() as LensPlayerFact.Change).properties.hasMediaData)
    }

    @Test
    fun snapshot_readsThePlayer() {
        subscribe()
        setSource(SourceDescription(listOf(TypedSource("https://cdn.example.com/a.mp4"))))
        whenever(player.currentTime).thenReturn(12.5)
        whenever(player.duration).thenReturn(60.0)
        doReturn(timeRanges(10.0 to 20.0)).whenever(player).buffered
        whenever(view.width).thenReturn(640)
        whenever(view.height).thenReturn(360)

        val snapshot = adapter.snapshot()

        assertEquals(12.5, snapshot.currentTime, 0.0)
        assertEquals(60.0, snapshot.duration)
        assertEquals(20.0, snapshot.buffered?.single()?.ranges?.single()?.end)
        assertEquals(640, snapshot.viewWidth)
        assertEquals(360, snapshot.viewHeight)
    }

    @Test
    fun closingTheSubscription_removesEveryListener() {
        subscribe().close()

        assertEquals(0, dispatchers.count)
    }

    @Test
    fun destroy_removesTheSourceAndEveryListener() {
        whenever(player.source).thenReturn(SourceDescription(listOf(TypedSource("https://cdn.example.com/a.m3u8"))))
        subscribe()

        dispatchers.fire(PlayerEventTypes.DESTROY)

        assertNull((facts.last() as LensPlayerFact.Source).source)
        assertEquals(0, dispatchers.count)
    }
}
