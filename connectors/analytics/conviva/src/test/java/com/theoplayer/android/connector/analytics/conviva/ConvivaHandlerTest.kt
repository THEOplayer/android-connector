package com.theoplayer.android.connector.analytics.conviva

import android.content.Context
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.conviva.sdk.ConvivaSdkConstants.StreamType
import com.theoplayer.android.api.abr.Abr
import com.theoplayer.android.api.abr.AbrStrategyConfiguration
import com.theoplayer.android.api.abr.AbrStrategyType
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.EventType
import com.theoplayer.android.api.event.ads.AdEvent
import com.theoplayer.android.api.event.ads.AdIntegrationKind
import com.theoplayer.android.api.event.player.DurationChangeEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.TimeUpdateEvent
import com.theoplayer.android.api.network.Network
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.theolive.TheoLive
import com.theoplayer.android.api.ads.theoads.theoAds
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.addescription.AdDescription
import com.theoplayer.android.api.source.metadata.MetadataDescription
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

private const val CONTENT_DURATION = 596
private const val AD_DURATION = 10

class ConvivaHandlerTest {

    private val playerEvents = TestEventDispatcher<PlayerEvent<*>>()
    private val adEvents = TestEventDispatcher<AdEvent<*>>()
    private lateinit var ads: FakeAds
    private lateinit var player: Player
    private lateinit var videoAnalytics: ConvivaVideoAnalytics
    private lateinit var convivaStatic: MockedStatic<ConvivaAnalytics>
    private lateinit var theoAdsMock: MockedStatic<*>
    private var duration: Double = Double.NaN
    private var handler: ConvivaHandler? = null

    private fun adBreak(kind: AdIntegrationKind, timeOffset: Int = 0): AdBreak = mock {
        on { this.timeOffset } doReturn timeOffset
        on { integration } doReturn kind
        on { this.ads } doReturn emptyList<Ad>()
    }

    @Before
    fun setUp() {
        ads = FakeAds(adEvents)
        videoAnalytics = mock()
        val typedSource = mock<TypedSource> {
            on { src } doReturn "content.m3u8"
        }
        val source = mock<SourceDescription> {
            on { this.sources } doReturn listOf(typedSource)
            on { this.ads } doReturn emptyList<AdDescription>()
            on { metadata } doReturn MetadataDescription(mutableMapOf("title" to "Content"))
        }
        val abrStrategy = mock<AbrStrategyConfiguration> {
            on { type } doReturn AbrStrategyType.BANDWIDTH
            on { metadata } doReturn null
        }
        val abr = mock<Abr> {
            on { targetBuffer } doReturn 20
            on { this.abrStrategy } doReturn abrStrategy
        }
        val network = mock<Network>()
        val theoLive = mock<TheoLive>()
        player = mock {
            on { this.ads } doReturn ads
            on { this.network } doReturn network
            on { this.theoLive } doReturn theoLive
            on { this.abr } doReturn abr
            on { this.source } doReturn source
            on { src } doReturn "content.m3u8"
        }
        whenever(player.duration).thenAnswer { duration }
        whenever(player.addEventListener(any<EventType<PlayerEvent<*>>>(), any())).thenAnswer {
            playerEvents.addEventListener(
                it.getArgument<EventType<PlayerEvent<*>>>(0),
                it.getArgument<EventListener<in PlayerEvent<*>>>(1)
            )
            null
        }
        whenever(player.removeEventListener(any<EventType<PlayerEvent<*>>>(), any())).thenAnswer {
            playerEvents.removeEventListener(
                it.getArgument<EventType<PlayerEvent<*>>>(0),
                it.getArgument<EventListener<in PlayerEvent<*>>>(1)
            )
            null
        }
        theoAdsMock = mockStatic(Class.forName("com.theoplayer.android.api.ads.theoads.TheoAdsIntegrationKt"))
        theoAdsMock.`when`<Any?> { player.theoAds }.thenThrow(IllegalStateException())
        convivaStatic = mockStatic(ConvivaAnalytics::class.java)
        convivaStatic.`when`<ConvivaVideoAnalytics> { ConvivaAnalytics.buildVideoAnalytics(any()) }
            .thenReturn(videoAnalytics)
        convivaStatic.`when`<ConvivaAdAnalytics> { ConvivaAnalytics.buildAdAnalytics(any(), any()) }
            .thenReturn(mock())
    }

    @After
    fun tearDown() {
        handler?.destroy()
        handler = null
        convivaStatic.close()
        theoAdsMock.close()
    }

    private fun createHandler(convivaMetadata: Map<String, Any> = emptyMap()) {
        handler = ConvivaHandler(
            mock<Context>(),
            player,
            convivaMetadata,
            ConvivaConfiguration("test", true, null),
            null
        )
    }

    private fun contentInfoCalls(): List<Map<String, Any>> {
        val captor = argumentCaptor<Map<String, Any>>()
        verify(videoAnalytics, atLeast(0)).setContentInfo(captor.capture())
        return captor.allValues
    }

    private fun callsWithDuration(): List<Map<String, Any>> =
        contentInfoCalls().filter { it.containsKey(ConvivaSdkConstants.DURATION) }

    private fun play(d: Double = Double.NaN) {
        duration = d
        playerEvents.dispatchEvent(PlayerEventTypes.PLAY, mock<PlayEvent>())
    }

    private fun durationchange(d: Double) {
        duration = d
        playerEvents.dispatchEvent(PlayerEventTypes.DURATIONCHANGE, mock<DurationChangeEvent>())
    }

    private fun timeupdate() {
        playerEvents.dispatchEvent(PlayerEventTypes.TIMEUPDATE, mock<TimeUpdateEvent>())
    }

    @Test
    fun playWhileAdRouted_reportsMetadataBeforePlaybackRequested() {
        createHandler()
        ads.playing = true
        play()
        inOrder(videoAnalytics).apply {
            verify(videoAnalytics).setContentInfo(any())
            verify(videoAnalytics).reportPlaybackRequested(any())
        }
        assertTrue(contentInfoCalls().isNotEmpty())
        assertEquals(
            "Content",
            contentInfoCalls().last()[ConvivaSdkConstants.ASSET_NAME]
        )
        assertTrue(callsWithDuration().isEmpty())

        durationchange(AD_DURATION.toDouble())
        timeupdate()
        durationchange(Double.NaN)
        assertTrue(callsWithDuration().isEmpty())

        ads.playing = false
        ads.breakInProgress = null
        durationchange(CONTENT_DURATION.toDouble())
        assertEquals(
            CONTENT_DURATION,
            contentInfoCalls().last()[ConvivaSdkConstants.DURATION]
        )
        assertTrue(callsWithDuration().none { it[ConvivaSdkConstants.DURATION] == AD_DURATION })
        timeupdate()
        assertEquals(1, callsWithDuration().size)
    }

    @Test
    fun timeupdateAfterBreak_reportsContentDurationOnce() {
        createHandler()
        play()
        ads.playing = true
        ads.breakInProgress = adBreak(AdIntegrationKind.GOOGLE_IMA)
        durationchange(AD_DURATION.toDouble())
        durationchange(CONTENT_DURATION.toDouble())
        ads.playing = false
        ads.breakInProgress = null
        timeupdate()
        assertEquals(
            CONTENT_DURATION,
            contentInfoCalls().last()[ConvivaSdkConstants.DURATION]
        )
        timeupdate()
        assertEquals(1, callsWithDuration().size)
    }

    @Test
    fun durationchangeAfterBreak_reportsDeferredContentDuration() {
        createHandler()
        play()
        ads.playing = true
        ads.breakInProgress = adBreak(AdIntegrationKind.GOOGLE_IMA)
        durationchange(AD_DURATION.toDouble())
        ads.playing = false
        ads.breakInProgress = null
        duration = Double.NaN
        timeupdate()
        assertTrue(callsWithDuration().isEmpty())
        durationchange(CONTENT_DURATION.toDouble())
        assertEquals(
            CONTENT_DURATION,
            contentInfoCalls().last()[ConvivaSdkConstants.DURATION]
        )
    }

    @Test
    fun midrollBreak_keepsKnownContentDuration() {
        createHandler()
        play(CONTENT_DURATION.toDouble())
        durationchange(CONTENT_DURATION.toDouble())
        ads.playing = true
        ads.breakInProgress = adBreak(AdIntegrationKind.GOOGLE_IMA, 300)
        durationchange(AD_DURATION.toDouble())
        assertTrue(callsWithDuration().none { it[ConvivaSdkConstants.DURATION] == AD_DURATION })

        // The VMAP midroll case: the content durationchange can arrive while the ad still owns
        // playback; it must not re-report yet.
        durationchange(CONTENT_DURATION.toDouble())
        ads.playing = false
        ads.breakInProgress = null
        val callsBeforeTimeupdate = contentInfoCalls().size
        timeupdate()
        assertEquals(
            CONTENT_DURATION,
            contentInfoCalls().last()[ConvivaSdkConstants.DURATION]
        )
        assertEquals(callsBeforeTimeupdate + 1, contentInfoCalls().size)
    }

    @Test
    fun customBreak_isTreatedAsServerSide_reportsDurationImmediately() {
        createHandler()
        play()
        ads.playing = true
        ads.breakInProgress = adBreak(AdIntegrationKind.CUSTOM)
        durationchange(CONTENT_DURATION.toDouble())
        assertEquals(
            CONTENT_DURATION,
            contentInfoCalls().last()[ConvivaSdkConstants.DURATION]
        )
    }

    @Test
    fun googleDaiBreak_reportsStreamDurationImmediately() {
        createHandler()
        play()
        ads.playing = true
        ads.breakInProgress = adBreak(AdIntegrationKind.GOOGLE_DAI)
        durationchange(CONTENT_DURATION.toDouble())
        assertEquals(
            CONTENT_DURATION,
            contentInfoCalls().last()[ConvivaSdkConstants.DURATION]
        )
    }

    @Test
    fun prerollOverLive_reportsLiveAfterBreak() {
        createHandler()
        play()
        ads.playing = true
        durationchange(AD_DURATION.toDouble())
        assertTrue(contentInfoCalls().none { it[ConvivaSdkConstants.IS_LIVE] == StreamType.VOD })
        ads.playing = false
        durationchange(Double.POSITIVE_INFINITY)
        assertEquals(StreamType.LIVE, contentInfoCalls().last()[ConvivaSdkConstants.IS_LIVE])
        assertTrue(callsWithDuration().isEmpty())
    }

    @Test
    fun configuredIsLive_reportsWhileAdRouted() {
        whenever(videoAnalytics.metadataInfo).thenReturn(mapOf(ConvivaSdkConstants.IS_LIVE to StreamType.LIVE))
        createHandler()
        play()
        ads.playing = true
        durationchange(AD_DURATION.toDouble())
        assertEquals(StreamType.LIVE, contentInfoCalls().last()[ConvivaSdkConstants.IS_LIVE])
        assertTrue(callsWithDuration().isEmpty())
    }

    @Test
    fun durationchangeWithoutAds_reportsDuration() {
        createHandler()
        play()
        durationchange(CONTENT_DURATION.toDouble())
        assertEquals(
            CONTENT_DURATION,
            contentInfoCalls().last()[ConvivaSdkConstants.DURATION]
        )
    }

    @Test
    fun playbackFailedDuringPreroll_reportsMinusOneContentLength() {
        createHandler()
        play()
        ads.playing = true
        ads.breakInProgress = adBreak(AdIntegrationKind.GOOGLE_IMA)
        durationchange(AD_DURATION.toDouble())
        handler!!.reportPlaybackFailed("error")
        verify(videoAnalytics).reportPlaybackFailed(
            eq("error"),
            eq(mapOf<String, Any>(ConvivaSdkConstants.DURATION to -1))
        )
    }

    @Test
    fun playbackFailedDuringMidroll_keepsKnownContentDuration() {
        createHandler()
        play(CONTENT_DURATION.toDouble())
        durationchange(CONTENT_DURATION.toDouble())
        ads.playing = true
        ads.breakInProgress = adBreak(AdIntegrationKind.GOOGLE_IMA, 300)
        durationchange(AD_DURATION.toDouble())
        handler!!.reportPlaybackFailed("error")
        verify(videoAnalytics).reportPlaybackFailed("error")
        verify(videoAnalytics, never()).reportPlaybackFailed(eq("error"), any())
    }

    @Test
    fun playbackFailedWithNanDuration_reportsMinusOne() {
        createHandler()
        play(CONTENT_DURATION.toDouble())
        durationchange(CONTENT_DURATION.toDouble())
        duration = Double.NaN
        handler!!.reportPlaybackFailed("error")
        verify(videoAnalytics).reportPlaybackFailed(
            eq("error"),
            eq(mapOf<String, Any>(ConvivaSdkConstants.DURATION to -1))
        )
    }

    @Test
    fun playbackFailedDuringContent_reportsNoDurationOverride() {
        createHandler()
        play(CONTENT_DURATION.toDouble())
        durationchange(CONTENT_DURATION.toDouble())
        handler!!.reportPlaybackFailed("error")
        verify(videoAnalytics).reportPlaybackFailed("error")
        verify(videoAnalytics, never()).reportPlaybackFailed(eq("error"), any())
    }

    @Test
    fun playbackFailedDuringPreroll_withConfiguredDuration_reportsNoOverride() {
        createHandler(mapOf(ConvivaSdkConstants.DURATION to CONTENT_DURATION))
        play()
        ads.playing = true
        ads.breakInProgress = adBreak(AdIntegrationKind.GOOGLE_IMA)
        durationchange(AD_DURATION.toDouble())
        handler!!.reportPlaybackFailed("error")
        verify(videoAnalytics).reportPlaybackFailed("error")
        verify(videoAnalytics, never()).reportPlaybackFailed(eq("error"), any())
    }

    @Test
    fun timeupdateBeforePlay_doesNotReport() {
        createHandler()
        ads.playing = true
        durationchange(AD_DURATION.toDouble())
        ads.playing = false
        duration = CONTENT_DURATION.toDouble()
        timeupdate()
        assertTrue(callsWithDuration().isEmpty())
        verify(videoAnalytics, never()).reportPlaybackRequested(any())
    }
}
