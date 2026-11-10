package com.theoplayer.android.connector.analytics.conviva.ads

import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.Ads
import com.theoplayer.android.api.ads.Omid
import com.theoplayer.android.api.ads.ServerSideAdIntegrationFactory
import com.theoplayer.android.api.ads.ima.GoogleImaAdEventType
import com.theoplayer.android.api.ads.theoads.Interstitial
import com.theoplayer.android.api.ads.theoads.InterstitialType
import com.theoplayer.android.api.ads.theoads.TheoAdsIntegration
import com.theoplayer.android.api.ads.theoads.event.InterstitialErrorEvent
import com.theoplayer.android.api.ads.theoads.event.TheoAdsEventTypes
import com.theoplayer.android.api.ads.theoads.theoAds
import com.theoplayer.android.api.event.Event
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.EventType
import com.theoplayer.android.api.event.ads.AdBeginEvent
import com.theoplayer.android.api.event.ads.AdBreakBeginEvent
import com.theoplayer.android.api.event.ads.AdBreakEndEvent
import com.theoplayer.android.api.event.ads.AdEndEvent
import com.theoplayer.android.api.event.ads.AdEvent
import com.theoplayer.android.api.event.ads.AdIntegrationKind
import com.theoplayer.android.api.event.ads.AdsEventTypes
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.addescription.AdDescription
import com.theoplayer.android.connector.analytics.conviva.ConvivaHandlerBase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

class AdReporterTest {

    private class FakeAds(private val listeners: MutableMap<EventType<*>, EventListener<*>>) : Ads {
        var breakInProgress: AdBreak? = null
        var scheduled: List<Ad> = emptyList()
        override fun isPlaying() = false
        override fun getCurrentAds(): List<Ad> = emptyList()
        override fun getCurrentAdBreak(): AdBreak? = breakInProgress
        override fun getScheduledAds(): List<Ad> = scheduled
        override fun schedule(desc: AdDescription) {}
        override fun skip() {}
        override fun getOmid(): Omid = throw UnsupportedOperationException()
        override fun registerServerSideIntegration(id: String, factory: ServerSideAdIntegrationFactory) {}
        override fun <T : AdEvent<*>> addEventListener(type: EventType<T>, listener: EventListener<in T>) {
            listeners[type] = listener
        }
        override fun <T : AdEvent<*>> removeEventListener(type: EventType<T>, listener: EventListener<in T>) {
            listeners.remove(type)
        }
    }

    private val listeners = mutableMapOf<EventType<*>, EventListener<*>>()
    private lateinit var ads: FakeAds
    private lateinit var player: Player
    private lateinit var videoAnalytics: ConvivaVideoAnalytics
    private lateinit var adAnalytics: ConvivaAdAnalytics
    private lateinit var handler: ConvivaHandlerBase
    private lateinit var theoAdsMock: MockedStatic<*>
    private lateinit var reporter: AdReporter

    private fun adBreak(timeOffset: Int, adTypes: List<String> = listOf("linear")): AdBreak {
        val adsList = adTypes.map { adType ->
            mock<Ad> {
                on { id } doReturn "ad-$adType"
                on { type } doReturn adType
                on { integration } doReturn AdIntegrationKind.GOOGLE_IMA
            }
        }
        return mock {
            on { this.timeOffset } doReturn timeOffset
            on { maxDuration } doReturn 30
            on { this.ads } doReturn adsList
            on { integration } doReturn AdIntegrationKind.GOOGLE_IMA
        }
    }

    private fun linearAd(brk: AdBreak?): Ad = mock {
        on { id } doReturn "ad-1"
        on { type } doReturn "linear"
        on { adBreak } doReturn brk
        on { integration } doReturn AdIntegrationKind.GOOGLE_IMA
    }

    @Before
    fun setUp() {
        listeners.clear()
        ads = FakeAds(listeners)
        videoAnalytics = mock {
            on { sessionId } doReturn 42
        }
        adAnalytics = mock()
        handler = mock {
            on { contentAssetName } doReturn "asset"
        }
        player = mock {
            on { this.ads } doReturn ads
            on { videoWidth } doReturn 1920
            on { videoHeight } doReturn 1080
        }
        doAnswer { inv ->
            listeners[inv.getArgument(0)] = inv.getArgument(1)
            null
        }.`when`(player).addEventListener(any<EventType<PlayerEvent<*>>>(), any())
        theoAdsMock = mockStatic(Class.forName("com.theoplayer.android.api.ads.theoads.TheoAdsIntegrationKt"))
        theoAdsMock.`when`<TheoAdsIntegration?> { player.theoAds }.thenThrow(IllegalStateException())
        reporter = AdReporter(player, videoAnalytics, adAnalytics, handler, null)
    }

    @After
    fun tearDown() {
        theoAdsMock.close()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E : Event<*>> fire(type: EventType<E>, event: E) {
        (listeners[type] as? EventListener<E>)?.handleEvent(event)
    }

    private fun fireBreakBegin(brk: AdBreak) {
        fire(AdsEventTypes.AD_BREAK_BEGIN, mock<AdBreakBeginEvent> { on { this.adBreak } doReturn brk })
    }

    private fun fireAdBegin(ad: Ad) {
        fire(AdsEventTypes.AD_BEGIN, mock<AdBeginEvent> { on { this.ad } doReturn ad })
    }

    private fun fireAdEnd(ad: Ad) {
        fire(AdsEventTypes.AD_END, mock<AdEndEvent> { on { this.ad } doReturn ad })
    }

    private fun fireBreakEnd() {
        fire(AdsEventTypes.AD_BREAK_END, mock<AdBreakEndEvent>())
    }

    private fun fireAdError(ad: Ad?, message: String?) {
        fire(AdsEventTypes.AD_ERROR, mock {
            on { this.ad } doReturn ad
            on { error } doReturn message
        })
    }

    private fun podInfo(position: String, index: Int) = mapOf(
        ConvivaSdkConstants.POD_POSITION to position,
        ConvivaSdkConstants.POD_DURATION to 30,
        ConvivaSdkConstants.POD_INDEX to index
    )

    private fun fallbackPodInfo(position: String, index: Int) = mapOf(
        ConvivaSdkConstants.POD_POSITION to position,
        ConvivaSdkConstants.POD_DURATION to 0,
        ConvivaSdkConstants.POD_INDEX to index
    )

    private fun expectedErrorAdInfo(technology: String = "Client Side") = mapOf(
        "c3.csid" to "42",
        "contentAssetName" to "asset",
        "c3.ad.technology" to technology,
        ConvivaSdkConstants.IS_LIVE to false
    )

    @Test
    fun a_breakBeginThenAdLifecycle_reportsOnePod() {
        val brk = adBreak(0)
        val ad = linearAd(brk)
        fireBreakBegin(brk)
        fireAdBegin(ad)
        fireAdEnd(ad)
        fireBreakEnd()
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.CLIENT_SIDE),
                eq(podInfo("PREROLL", 1))
            )
            verify(adAnalytics).setAdInfo(any())
            verify(adAnalytics).reportAdLoaded(any())
            verify(adAnalytics).reportAdStarted(any())
            verify(adAnalytics).reportAdEnded()
            verify(videoAnalytics).reportAdBreakEnded()
        }
        verify(videoAnalytics, times(1)).reportAdBreakStarted(any(), any(), any<Map<String, Any>>())
        verify(videoAnalytics, times(1)).reportAdBreakEnded()
    }

    @Test
    fun b_adErrorWhilePodOpenWithoutAd_setsAdInfoThenReportsFailure() {
        val brk = adBreak(0)
        fireBreakBegin(brk)
        fireAdError(null, "media fail")
        fireBreakEnd()
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(any(), any(), any())
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo()))
            verify(adAnalytics).reportAdFailed("media fail")
            verify(videoAnalytics).reportAdBreakEnded()
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun c_adErrorWithoutBreak_reportsErrorPodWithFallbackInfo() {
        fireAdError(null, "VAST timeout")
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.CLIENT_SIDE),
                eq(fallbackPodInfo("PREROLL", 1))
            )
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo()))
            verify(adAnalytics).reportAdFailed("VAST timeout")
            verify(videoAnalytics).reportAdBreakEnded()
        }
    }

    @Test
    fun d_genericAndImaErrorForOneFailure_reportsOneSequence() {
        // The SDK forwards one IMA failure as GoogleImaAdEventType.AD_BREAK_FETCH_ERROR,
        // GoogleImaAdEventType.AD_ERROR and a generic AdsEventTypes.AD_ERROR; only the
        // generic event has a listener, so the failure is reported exactly once.
        fireAdError(null, "boom")
        fire(GoogleImaAdEventType.AD_BREAK_FETCH_ERROR, mock())
        fire(GoogleImaAdEventType.AD_ERROR, mock())
        verify(videoAnalytics, times(1)).reportAdBreakStarted(any(), any(), any<Map<String, Any>>())
        verify(adAnalytics, times(1)).setAdInfo(any())
        verify(adAnalytics, times(1)).reportAdFailed("boom")
        verify(videoAnalytics, times(1)).reportAdBreakEnded()
    }

    @Test
    fun e_adBeginWithoutBreakBegin_opensPodOnce() {
        val brk = adBreak(30)
        val ad = linearAd(brk)
        fireAdBegin(ad)
        fireBreakBegin(brk)
        verify(videoAnalytics, times(1)).reportAdBreakStarted(
            eq(ConvivaSdkConstants.AdPlayer.CONTENT),
            eq(ConvivaSdkConstants.AdType.CLIENT_SIDE),
            eq(podInfo("MIDROLL", 1))
        )
        verify(adAnalytics, times(1)).reportAdStarted(any())
    }

    @Test
    fun f_playerEventsBeforeAdBegin_reportNoAdMetrics() {
        fireBreakBegin(adBreak(0))
        fire(PlayerEventTypes.PLAY, mock<PlayEvent>())
        fire(PlayerEventTypes.PLAYING, mock<PlayingEvent>())
        fire(PlayerEventTypes.PAUSE, mock<PauseEvent>())
        fire(GoogleImaAdEventType.AD_BUFFERING, mock())
        fire(AdsEventTypes.AD_SKIP, mock())
        verify(adAnalytics, times(0)).reportAdMetric(eq(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE), any())
    }

    @Test
    fun g_secondBreakEnd_reportsNoSecondPodEnd() {
        fireBreakBegin(adBreak(0))
        fireBreakEnd()
        fireBreakEnd()
        verify(videoAnalytics, times(1)).reportAdBreakEnded()
    }

    @Test
    fun h_nonLinearBreak_reportsNothing() {
        fireBreakBegin(adBreak(0, listOf("nonlinear")))
        fireBreakEnd()
        verify(videoAnalytics, times(0)).reportAdBreakStarted(any(), any(), any<Map<String, Any>>())
        verify(videoAnalytics, times(0)).reportAdBreakEnded()
    }

    @Test
    fun i_destroyWithOpenPod_reportsEndsOnce() {
        val brk = adBreak(0)
        val ad = linearAd(brk)
        fireBreakBegin(brk)
        fireAdBegin(ad)
        reporter.destroy()
        verify(adAnalytics, times(1)).reportAdEnded()
        verify(videoAnalytics, times(1)).reportAdBreakEnded()
    }

    @Test
    fun j_interstitialError_reportsUnchangedSgaiErrorPod() {
        theoAdsMock.close()
        val theoAds = mock<TheoAdsIntegration>()
        doAnswer { inv ->
            listeners[inv.getArgument(0)] = inv.getArgument(1)
            null
        }.`when`(theoAds).addEventListener(any(), any())
        theoAdsMock = mockStatic(Class.forName("com.theoplayer.android.api.ads.theoads.TheoAdsIntegrationKt"))
        theoAdsMock.`when`<TheoAdsIntegration?> { player.theoAds }.thenReturn(theoAds)
        reporter = AdReporter(player, videoAnalytics, adAnalytics, handler, null)

        val interstitial = mock<Interstitial> {
            on { type } doReturn InterstitialType.ADBREAK
            on { startTime } doReturn 30.0
            on { duration } doReturn 30.0
        }
        @Suppress("UNCHECKED_CAST")
        (listeners[TheoAdsEventTypes.INTERSTITIAL_ERROR] as? EventListener<InterstitialErrorEvent>)
            ?.handleEvent(mock {
                on { this.interstitial } doReturn interstitial
                on { message } doReturn "no fill"
            })

        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.SERVER_SIDE),
                eq(podInfo("MIDROLL", 1))
            )
            verify(adAnalytics).setAdInfo(any())
            verify(adAnalytics).reportAdFailed("no fill")
            verify(videoAnalytics).reportAdBreakEnded()
        }
    }

    @Test
    fun k_breakPositions_mapToEnumNames() {
        listOf(0 to "PREROLL", 30 to "MIDROLL", -1 to "POSTROLL").forEachIndexed { i, (offset, position) ->
            val r = AdReporter(player, videoAnalytics, adAnalytics, handler, null)
            fireBreakBegin(adBreak(offset))
            val captor = argumentCaptor<Map<String, Any>>()
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                any(),
                captor.capture()
            )
            assertEquals(position, captor.lastValue[ConvivaSdkConstants.POD_POSITION])
            assertEquals(1, captor.lastValue[ConvivaSdkConstants.POD_INDEX])
            r.reset()
            clearInvocations(videoAnalytics)
        }
    }

    @Test
    fun l_resetWithOpenAdAndPod_reportsEndsOnce() {
        val brk = adBreak(0)
        val ad = linearAd(brk)
        fireBreakBegin(brk)
        fireAdBegin(ad)
        reporter.reset()
        fireBreakEnd()

        verify(adAnalytics, times(1)).reportAdEnded()
        verify(videoAnalytics, times(1)).reportAdBreakEnded()
    }

    @Test
    fun m_contentResumeRequested_closesPodOnce() {
        fireBreakBegin(adBreak(0))
        fire(GoogleImaAdEventType.CONTENT_RESUME_REQUESTED, mock())
        fire(GoogleImaAdEventType.CONTENT_RESUME_REQUESTED, mock())

        verify(videoAnalytics, times(1)).reportAdBreakStarted(any(), any(), any<Map<String, Any>>())
        verify(videoAnalytics, times(1)).reportAdBreakEnded()
    }

    @Test
    fun n_adEventsExtensionDispatcher_alsoOpensPod() {
        val extListeners = mutableMapOf<EventType<*>, EventListener<*>>()
        val extAds = FakeAds(extListeners)
        reporter = AdReporter(player, videoAnalytics, adAnalytics, handler, extAds)

        val brk = adBreak(0)
        val ad = linearAd(brk)
        @Suppress("UNCHECKED_CAST")
        (extListeners[AdsEventTypes.AD_BREAK_BEGIN] as? EventListener<AdBreakBeginEvent>)
            ?.handleEvent(mock { on { this.adBreak } doReturn brk })
        @Suppress("UNCHECKED_CAST")
        (extListeners[AdsEventTypes.AD_BEGIN] as? EventListener<AdBeginEvent>)
            ?.handleEvent(mock { on { this.ad } doReturn ad })

        verify(videoAnalytics, times(1)).reportAdBreakStarted(any(), any(), any<Map<String, Any>>())
        verify(adAnalytics, times(1)).reportAdStarted(any())
    }


    @Test
    fun o_adErrorWithoutMessage_reportsDefaultMessage() {
        fireAdError(null, null)
        verify(adAnalytics, times(1)).reportAdFailed("Ad Request Failed")
    }

    @Test
    fun p_playAfterBreakEnd_reportsNoAdMetric() {
        val brk = adBreak(0)
        fireBreakBegin(brk)
        fireAdBegin(linearAd(brk))
        fireBreakEnd()
        clearInvocations(videoAnalytics, adAnalytics)

        fire(PlayerEventTypes.PLAY, mock<PlayEvent>())
        verify(adAnalytics, times(0)).reportAdMetric(eq(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE), any())
    }
}