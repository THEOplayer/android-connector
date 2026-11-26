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
import com.theoplayer.android.api.ads.theoads.event.TheoAdsEvent
import com.theoplayer.android.api.ads.theoads.event.TheoAdsEventTypes
import com.theoplayer.android.api.ads.theoads.theoAds
import com.theoplayer.android.api.event.EventDispatcher
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
import com.theoplayer.android.connector.analytics.conviva.TestEventDispatcher
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AdReporterTest {

    private class FakeAds(dispatcher: TestEventDispatcher<AdEvent<*>>) :
        Ads, EventDispatcher<AdEvent<*>> by dispatcher {
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
    }

    private val adEvents = TestEventDispatcher<AdEvent<*>>()
    private val playerEvents = TestEventDispatcher<PlayerEvent<*>>()
    private lateinit var ads: FakeAds
    private lateinit var player: Player
    private lateinit var videoAnalytics: ConvivaVideoAnalytics
    private lateinit var adAnalytics: ConvivaAdAnalytics
    private lateinit var handler: ConvivaHandlerBase
    private lateinit var theoAdsMock: MockedStatic<*>
    private lateinit var reporter: AdReporter

    private fun adBreak(
        timeOffset: Int,
        adTypes: List<String> = listOf("linear"),
        integrationKind: AdIntegrationKind = AdIntegrationKind.GOOGLE_IMA
    ): AdBreak {
        val adsList = adTypes.map { adType ->
            mock<Ad> {
                on { id } doReturn "ad-$adType"
                on { type } doReturn adType
                on { integration } doReturn integrationKind
            }
        }
        return mock {
            on { this.timeOffset } doReturn timeOffset
            on { maxDuration } doReturn 30
            on { this.ads } doReturn adsList
            on { integration } doReturn integrationKind
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
        ads = FakeAds(adEvents)
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
        whenever(player.addEventListener(any<EventType<PlayerEvent<*>>>(), any())).thenAnswer {
            playerEvents.addEventListener(
                it.getArgument<EventType<PlayerEvent<*>>>(0),
                it.getArgument<EventListener<in PlayerEvent<*>>>(1)
            )
            null
        }
        theoAdsMock = mockStatic(Class.forName("com.theoplayer.android.api.ads.theoads.TheoAdsIntegrationKt"))
        theoAdsMock.`when`<TheoAdsIntegration?> { player.theoAds }.thenThrow(IllegalStateException())
        reporter = AdReporter(player, videoAnalytics, adAnalytics, handler, null)
    }

    @After
    fun tearDown() {
        theoAdsMock.close()
    }

    private fun <E : AdEvent<*>> fire(type: EventType<E>, event: E) {
        adEvents.dispatchEvent(type, event)
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

    private fun expectedErrorAdInfo(technology: String = "Client Side", position: String) = mapOf(
        "c3.csid" to "42",
        "contentAssetName" to "asset",
        "c3.ad.technology" to technology,
        "c3.ad.position" to position,
        ConvivaSdkConstants.IS_LIVE to false
    )

    @Test
    fun breakBeginThenAdLifecycle_reportsOnePod() {
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
    fun adErrorWhilePodOpenWithoutAd_setsAdInfoThenReportsFailure() {
        val brk = adBreak(0)
        fireBreakBegin(brk)
        fireAdError(null, "media fail")
        fireBreakEnd()
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(any(), any(), any())
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo(position = "Pre-roll")))
            verify(adAnalytics).reportAdFailed("media fail")
            verify(videoAnalytics).reportAdBreakEnded()
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun adErrorWhilePodOpenWithoutAd_midRoll_setsMidRollPosition() {
        fireBreakBegin(adBreak(30))
        fireAdError(null, "media fail")
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.CLIENT_SIDE),
                eq(podInfo("MIDROLL", 1))
            )
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo(position = "Mid-roll")))
            verify(adAnalytics).reportAdFailed("media fail")
        }
    }

    @Test
    fun adErrorWithKnownBreak_reportsErrorPodWithBreakPosition() {
        val brk = adBreak(30)
        fireAdError(linearAd(brk), "VAST 404")
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.CLIENT_SIDE),
                eq(podInfo("MIDROLL", 1))
            )
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo(position = "Mid-roll")))
            verify(adAnalytics).reportAdFailed("VAST 404")
            verify(videoAnalytics).reportAdBreakEnded()
        }
    }

    @Test
    fun adErrorWithoutBreak_reportsErrorPodWithFallbackInfo() {
        fireAdError(null, "VAST timeout")
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.CLIENT_SIDE),
                eq(fallbackPodInfo("PREROLL", 1))
            )
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo(position = "Pre-roll")))
            verify(adAnalytics).reportAdFailed("VAST timeout")
            verify(videoAnalytics).reportAdBreakEnded()
        }
    }

    @Test
    fun adErrorWithoutBreak_usesOnePositionForPodAndAdInfo() {
        // The fallback position must be classified once: a second read of currentTime
        // must not give the ad info a different position than the pod.
        whenever(player.currentTime).thenReturn(10.0, 0.0)
        fireAdError(null, "VAST timeout")
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.CLIENT_SIDE),
                eq(fallbackPodInfo("MIDROLL", 1))
            )
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo(position = "Mid-roll")))
            verify(adAnalytics).reportAdFailed("VAST timeout")
            verify(videoAnalytics).reportAdBreakEnded()
        }
    }

    @Test
    fun adErrorWithCurrentBreakFromPlayer_reportsErrorPodWithBreakPosition() {
        // No open pod and no ad in the error event, but the player still reports the
        // break in progress; its position is used for both the pod and the ad info.
        ads.breakInProgress = adBreak(30)
        fireAdError(null, "VAST 404")
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.CLIENT_SIDE),
                eq(podInfo("MIDROLL", 1))
            )
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo(position = "Mid-roll")))
            verify(adAnalytics).reportAdFailed("VAST 404")
            verify(videoAnalytics).reportAdBreakEnded()
        }
    }

    @Test
    fun adErrorWithScheduledTheoAdsBreak_reportsErrorPodAsServerGuided() {
        // A failed THEOads (SGAI) break is dispatched as a generic ad error without an
        // ad, but its scheduled break is still listed; use it for the pod and ad info.
        val theoAdsBreak = adBreak(20, integrationKind = AdIntegrationKind.THEO_ADS)
        ads.scheduled = listOf(mock { on { adBreak } doReturn theoAdsBreak })
        whenever(player.currentTime).thenReturn(1.0)
        fireAdError(null, "No Ads VAST response")
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.SERVER_SIDE),
                eq(podInfo("MIDROLL", 1))
            )
            verify(adAnalytics).setAdInfo(
                eq(expectedErrorAdInfo(technology = "Server Guided", position = "Mid-roll"))
            )
            verify(adAnalytics).reportAdFailed("No Ads VAST response")
            verify(videoAnalytics).reportAdBreakEnded()
        }
    }

    @Test
    fun adErrorWithScheduledImaBreak_usesItsInfo() {
        // A failed client-side break still scheduled near the playhead keeps its own
        // technology and position instead of the playhead heuristic.
        val imaBreak = adBreak(30)
        ads.scheduled = listOf(mock { on { adBreak } doReturn imaBreak })
        whenever(player.currentTime).thenReturn(10.0)
        fireAdError(null, "VAST timeout")
        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.CLIENT_SIDE),
                eq(podInfo("MIDROLL", 1))
            )
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo(position = "Mid-roll")))
            verify(adAnalytics).reportAdFailed("VAST timeout")
            verify(videoAnalytics).reportAdBreakEnded()
        }
    }

    @Test
    fun genericAndImaErrorForOneFailure_reportsOneSequence() {
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
    fun adBeginWithoutBreakBegin_opensPodOnce() {
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
    fun playerEventsBeforeAdBegin_reportNoAdMetrics() {
        fireBreakBegin(adBreak(0))
        playerEvents.dispatchEvent(PlayerEventTypes.PLAY, mock<PlayEvent>())
        playerEvents.dispatchEvent(PlayerEventTypes.PLAYING, mock<PlayingEvent>())
        playerEvents.dispatchEvent(PlayerEventTypes.PAUSE, mock<PauseEvent>())
        fire(GoogleImaAdEventType.AD_BUFFERING, mock())
        fire(AdsEventTypes.AD_SKIP, mock())
        verify(adAnalytics, times(0)).reportAdMetric(eq(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE), any())
    }

    @Test
    fun secondBreakEnd_reportsNoSecondPodEnd() {
        fireBreakBegin(adBreak(0))
        fireBreakEnd()
        fireBreakEnd()
        verify(videoAnalytics, times(1)).reportAdBreakEnded()
    }

    @Test
    fun nonLinearBreak_reportsNothing() {
        fireBreakBegin(adBreak(0, listOf("nonlinear")))
        fireBreakEnd()
        verify(videoAnalytics, times(0)).reportAdBreakStarted(any(), any(), any<Map<String, Any>>())
        verify(videoAnalytics, times(0)).reportAdBreakEnded()
    }

    @Test
    fun nonLinearAdError_reportsFailureWithoutPod() {
        val brk = adBreak(0, listOf("nonlinear"))
        fireAdError(brk.ads.first(), "overlay fail")
        verify(adAnalytics, times(1)).reportAdFailed("overlay fail")
        verify(videoAnalytics, times(0)).reportAdBreakStarted(any(), any(), any<Map<String, Any>>())
        verify(videoAnalytics, times(0)).reportAdBreakEnded()
    }

    @Test
    fun destroyWithOpenPod_reportsEndsOnce() {
        val brk = adBreak(0)
        val ad = linearAd(brk)
        fireBreakBegin(brk)
        fireAdBegin(ad)
        reporter.destroy()
        verify(adAnalytics, times(1)).reportAdEnded()
        verify(videoAnalytics, times(1)).reportAdBreakEnded()
    }

    private fun setUpTheoAds(): TestEventDispatcher<TheoAdsEvent<*>> {
        theoAdsMock.close()
        val theoAdsEvents = TestEventDispatcher<TheoAdsEvent<*>>()
        val theoAds = mock<TheoAdsIntegration>()
        whenever(theoAds.addEventListener(any(), any())).thenAnswer {
            theoAdsEvents.addEventListener(
                it.getArgument<EventType<TheoAdsEvent<*>>>(0),
                it.getArgument<EventListener<in TheoAdsEvent<*>>>(1)
            )
            null
        }
        theoAdsMock = mockStatic(Class.forName("com.theoplayer.android.api.ads.theoads.TheoAdsIntegrationKt"))
        theoAdsMock.`when`<TheoAdsIntegration?> { player.theoAds }.thenReturn(theoAds)
        return theoAdsEvents
    }

    private fun fireInterstitialError(
        theoAdsEvents: TestEventDispatcher<TheoAdsEvent<*>>,
        startTime: Double,
        message: String
    ) {
        val interstitial = mock<Interstitial> {
            on { type } doReturn InterstitialType.ADBREAK
            on { this.startTime } doReturn startTime
            on { duration } doReturn 30.0
        }
        theoAdsEvents.dispatchEvent(
            TheoAdsEventTypes.INTERSTITIAL_ERROR,
            mock<InterstitialErrorEvent> {
                on { this.interstitial } doReturn interstitial
                on { this.message } doReturn message
            }
        )
    }

    @Test
    fun interstitialError_reportsSgaiErrorPodWithPosition() {
        val theoAdsEvents = setUpTheoAds()
        reporter = AdReporter(player, videoAnalytics, adAnalytics, handler, null)

        fireInterstitialError(theoAdsEvents, 30.0, "no fill")

        inOrder(videoAnalytics, adAnalytics).apply {
            verify(videoAnalytics).reportAdBreakStarted(
                eq(ConvivaSdkConstants.AdPlayer.CONTENT),
                eq(ConvivaSdkConstants.AdType.SERVER_SIDE),
                eq(podInfo("MIDROLL", 1))
            )
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo("Server Guided", "Mid-roll")))
            verify(adAnalytics).reportAdFailed("no fill")
            verify(videoAnalytics).reportAdBreakEnded()
        }
    }

    @Test
    fun interstitialErrorPositions_mapToLabels() {
        // The default reporter is not registered on a THEOads dispatcher (the mocked player has
        // no THEOads integration), so each iteration only needs a fresh dispatcher + reporter.
        listOf(
            0.0 to ("PREROLL" to "Pre-roll"),
            -1.0 to ("POSTROLL" to "Post-roll"),
            Double.POSITIVE_INFINITY to ("POSTROLL" to "Post-roll")
        ).forEach { (startTime, expected) ->
            val theoAdsEvents = setUpTheoAds()
            AdReporter(player, videoAnalytics, adAnalytics, handler, null)
            fireInterstitialError(theoAdsEvents, startTime, "no fill")

            val podCaptor = argumentCaptor<Map<String, Any>>()
            verify(videoAnalytics).reportAdBreakStarted(any(), any(), podCaptor.capture())
            assertEquals(expected.first, podCaptor.lastValue[ConvivaSdkConstants.POD_POSITION])
            verify(adAnalytics).setAdInfo(eq(expectedErrorAdInfo("Server Guided", expected.second)))

            clearInvocations(videoAnalytics, adAnalytics)
        }
    }

    @Test
    fun breakPositions_mapToEnumNames() {
        // Each iteration registers a new reporter; release the default one so only the
        // iteration's reporter sees the dispatched event.
        reporter.destroy()
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
            r.destroy()
            clearInvocations(videoAnalytics)
        }
    }

    @Test
    fun resetWithOpenAdAndPod_reportsEndsOnce() {
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
    fun contentResumeRequested_closesPodOnce() {
        fireBreakBegin(adBreak(0))
        fire(GoogleImaAdEventType.CONTENT_RESUME_REQUESTED, mock())
        fire(GoogleImaAdEventType.CONTENT_RESUME_REQUESTED, mock())

        verify(videoAnalytics, times(1)).reportAdBreakStarted(any(), any(), any<Map<String, Any>>())
        verify(videoAnalytics, times(1)).reportAdBreakEnded()
    }

    @Test
    fun adEventsExtensionDispatcher_alsoOpensPod() {
        val extEvents = TestEventDispatcher<AdEvent<*>>()
        val extAds = FakeAds(extEvents)
        reporter = AdReporter(player, videoAnalytics, adAnalytics, handler, extAds)

        val brk = adBreak(0)
        val ad = linearAd(brk)
        extEvents.dispatchEvent(AdsEventTypes.AD_BREAK_BEGIN, mock<AdBreakBeginEvent> {
            on { this.adBreak } doReturn brk
        })
        extEvents.dispatchEvent(AdsEventTypes.AD_BEGIN, mock<AdBeginEvent> {
            on { this.ad } doReturn ad
        })

        verify(videoAnalytics, times(1)).reportAdBreakStarted(any(), any(), any<Map<String, Any>>())
        verify(adAnalytics, times(1)).reportAdStarted(any())
    }


    @Test
    fun adErrorWithoutMessage_reportsDefaultMessage() {
        fireAdError(null, null)
        verify(adAnalytics, times(1)).reportAdFailed("Ad Request Failed")
    }

    @Test
    fun playAfterBreakEnd_reportsNoAdMetric() {
        val brk = adBreak(0)
        fireBreakBegin(brk)
        fireAdBegin(linearAd(brk))
        fireBreakEnd()
        clearInvocations(videoAnalytics, adAnalytics)

        playerEvents.dispatchEvent(PlayerEventTypes.PLAY, mock<PlayEvent>())
        verify(adAnalytics, times(0)).reportAdMetric(eq(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE), any())
    }
}