package com.theoplayer.android.connector.analytics.conviva.utils

import com.conviva.sdk.ConvivaSdkConstants
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.theoads.Interstitial
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
}