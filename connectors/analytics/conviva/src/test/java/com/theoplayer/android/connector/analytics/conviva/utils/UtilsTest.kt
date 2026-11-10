package com.theoplayer.android.connector.analytics.conviva.utils

import com.conviva.sdk.ConvivaSdkConstants
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.Ads
import com.theoplayer.android.api.ads.theoads.Interstitial
import com.theoplayer.android.api.event.ads.AdIntegrationKind
import com.theoplayer.android.api.player.Player
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class UtilsTest {

    private fun adBreak(timeOffset: Int): AdBreak = mock {
        on { this.timeOffset } doReturn timeOffset
        on { maxDuration } doReturn 30
    }

    private fun interstitial(startTime: Double): Interstitial = mock {
        on { this.startTime } doReturn startTime
        on { duration } doReturn 30.0
    }

    private fun adWithBreak(timeOffset: Int): Ad {
        val breakMock = adBreak(timeOffset)
        return mock {
            on { id } doReturn "ad-id"
            on { adBreak } doReturn breakMock
        }
    }

    @Test
    fun calculateCurrentAdBreakInfo_reportsConvivaPodPositionEnumNames() {
        val cases = mapOf(0 to "PREROLL", 30 to "MIDROLL", -1 to "POSTROLL")
        cases.forEach { (timeOffset, expected) ->
            val info = calculateCurrentAdBreakInfo(adBreak(timeOffset), 1)
            val podPosition = info[ConvivaSdkConstants.POD_POSITION]
            assertEquals(expected, podPosition)
            // Conviva Core calls AdPosition.valueOf() on this value; must not throw.
            assertEquals(expected, ConvivaSdkConstants.AdPosition.valueOf(podPosition as String).name)
            assertEquals(30, info[ConvivaSdkConstants.POD_DURATION])
            assertEquals(1, info[ConvivaSdkConstants.POD_INDEX])
        }
    }

    @Test
    fun calculateInterstitialAdBreakInfo_reportsConvivaPodPositionEnumNames() {
        val cases = mapOf(
            0.0 to "PREROLL",
            30.0 to "MIDROLL",
            -1.0 to "POSTROLL",
            Double.NaN to "POSTROLL",
            Double.POSITIVE_INFINITY to "POSTROLL",
            Double.NEGATIVE_INFINITY to "POSTROLL"
        )
        cases.forEach { (startTime, expected) ->
            val info = calculateInterstitialAdBreakInfo(interstitial(startTime), 1)
            val podPosition = info[ConvivaSdkConstants.POD_POSITION]
            assertEquals("startTime=$startTime", expected, podPosition)
            assertEquals(expected, ConvivaSdkConstants.AdPosition.valueOf(podPosition as String).name)
            assertEquals(30, info[ConvivaSdkConstants.POD_DURATION])
            assertEquals(1, info[ConvivaSdkConstants.POD_INDEX])
        }
    }

    @Test
    fun collectAdMetadata_keepsC3AdPositionDisplayLabels() {
        val cases = mapOf(0 to "Pre-roll", 30 to "Mid-roll", -1 to "Post-roll")
        cases.forEach { (timeOffset, expected) ->
            assertEquals(expected, collectAdMetadata(adWithBreak(timeOffset))["c3.ad.position"])
        }
    }

    private fun scheduledAd(timeOffset: Int): Ad {
        val breakMock = adBreak(timeOffset)
        return mock {
            on { adBreak } doReturn breakMock
        }
    }

    private fun playerWithBreaks(offsets: List<Int>, currentTime: Double, isEnded: Boolean = false): Player {
        val scheduled = offsets.map { scheduledAd(it) }
        val adsApi = mock<Ads> {
            on { scheduledAds } doReturn scheduled
        }
        return mock {
            on { this.ads } doReturn adsApi
            on { this.currentTime } doReturn currentTime
            on { this.isEnded } doReturn isEnded
        }
    }

    @Test
    fun calculateCurrentAdBreakPosition_returnsAdPositionEnum() {
        val cases = mapOf(
            0 to ConvivaSdkConstants.AdPosition.PREROLL,
            30 to ConvivaSdkConstants.AdPosition.MIDROLL,
            -1 to ConvivaSdkConstants.AdPosition.POSTROLL
        )
        cases.forEach { (timeOffset, expected) ->
            assertEquals(expected, calculateCurrentAdBreakPosition(adBreak(timeOffset)))
        }
    }

    @Test
    fun calculateInterstitialAdBreakPosition_returnsAdPositionEnum() {
        val cases = mapOf(
            0.0 to ConvivaSdkConstants.AdPosition.PREROLL,
            30.0 to ConvivaSdkConstants.AdPosition.MIDROLL,
            -1.0 to ConvivaSdkConstants.AdPosition.POSTROLL,
            Double.NaN to ConvivaSdkConstants.AdPosition.POSTROLL,
            Double.POSITIVE_INFINITY to ConvivaSdkConstants.AdPosition.POSTROLL,
            Double.NEGATIVE_INFINITY to ConvivaSdkConstants.AdPosition.POSTROLL
        )
        cases.forEach { (startTime, expected) ->
            assertEquals("startTime=$startTime", expected, calculateInterstitialAdBreakPosition(interstitial(startTime)))
        }
    }

    @Test
    fun calculateFallbackAdBreakInfo_picksNearestScheduledBreak() {
        // Scheduled pre-roll (0) and mid-roll (30): the one nearest to the playhead wins.
        val info = calculateFallbackAdBreakInfo(playerWithBreaks(listOf(0, 30), 28.0), 2)
        assertEquals("MIDROLL", info[ConvivaSdkConstants.POD_POSITION])
        assertEquals(0, info[ConvivaSdkConstants.POD_DURATION])
        assertEquals(2, info[ConvivaSdkConstants.POD_INDEX])
        // Conviva Core calls AdPosition.valueOf() on this value; must not throw.
        ConvivaSdkConstants.AdPosition.valueOf(info[ConvivaSdkConstants.POD_POSITION] as String)

        val nearPre = calculateFallbackAdBreakInfo(playerWithBreaks(listOf(0, 30), 1.0), 1)
        assertEquals("PREROLL", nearPre[ConvivaSdkConstants.POD_POSITION])
    }

    @Test
    fun calculateFallbackAdBreakInfo_ignoresScheduledPostRolls() {
        // A scheduled post-roll (timeOffset -1) must not win a mid-stream lookup.
        val info = calculateFallbackAdBreakInfo(playerWithBreaks(listOf(-1), 10.0), 1)
        assertEquals("MIDROLL", info[ConvivaSdkConstants.POD_POSITION])
        ConvivaSdkConstants.AdPosition.valueOf(info[ConvivaSdkConstants.POD_POSITION] as String)
    }

    @Test
    fun calculateFallbackAdBreakInfo_endedWinsOverScheduledBreaks() {
        // After content ended the failure belongs to a post-roll, not the nearest mid-roll.
        val info = calculateFallbackAdBreakInfo(playerWithBreaks(listOf(0, 30), 10.0, isEnded = true), 1)
        assertEquals("POSTROLL", info[ConvivaSdkConstants.POD_POSITION])
    }

    @Test
    fun calculateFallbackAdBreakInfo_noScheduledBreaks_infersFromPlayback() {
        assertEquals(
            "PREROLL",
            calculateFallbackAdBreakInfo(playerWithBreaks(emptyList(), 0.0), 1)[ConvivaSdkConstants.POD_POSITION]
        )
        assertEquals(
            "POSTROLL",
            calculateFallbackAdBreakInfo(playerWithBreaks(emptyList(), 10.0, isEnded = true), 1)[ConvivaSdkConstants.POD_POSITION]
        )
        assertEquals(
            "MIDROLL",
            calculateFallbackAdBreakInfo(playerWithBreaks(emptyList(), 10.0), 1)[ConvivaSdkConstants.POD_POSITION]
        )
    }

    @Test
    fun calculateAdTypeAsString_mapsAdBreakIntegrationToLabel() {
        val cases = mapOf(
            AdIntegrationKind.THEO_ADS to SGAI_AD_TYPE,
            AdIntegrationKind.GOOGLE_IMA to "Client Side",
            AdIntegrationKind.GOOGLE_DAI to "Server Side"
        )
        cases.forEach { (integration, expected) ->
            val brk = mock<AdBreak> {
                on { this.integration } doReturn integration
            }
            assertEquals(expected, calculateAdTypeAsString(brk))
        }
    }
}
