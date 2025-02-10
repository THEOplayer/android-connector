package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.AdBreak
import com.theoplayer.android.api.ads.AdBreakInit
import com.theoplayer.android.api.ads.AdInit
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.connector.uplynk.network.UplynkAd
import com.theoplayer.android.connector.uplynk.network.UplynkAdBreak
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class AdHandlerTest {

    @Mock
    private lateinit var mockAd: Ad

    @Mock
    private lateinit var mockAdBreak: AdBreak

    @Mock
    private lateinit var controller: ServerSideAdIntegrationController
    private lateinit var adHandler: AdHandler

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(controller.createAdBreak(any())).thenReturn(mockAdBreak)
        whenever(controller.createAd(any(), any())).thenReturn(mockAd)
        adHandler = AdHandler(controller, 0)
    }

    @Test
    fun createAdBreak_always_callsControllerCreateAdBreak() {
        val adBreak = UplynkAdBreak(
            listOf(),
            "",
            "",
            100.toDuration(DurationUnit.SECONDS),
            200.toDuration(DurationUnit.SECONDS)
        )

        adHandler.createAdBreak(adBreak)

        verify(controller).createAdBreak(
            eq(
                AdBreakInit(
                    timeOffset = 100,
                    maxDuration = 200,
                    customData = adBreak
                )
            )
        )
    }

    @Test
    fun createAdBreak_withEmptyAds_neverCallsControllerCreateAd() {
        val adBreak = UplynkAdBreak(
            listOf(),
            "",
            "",
            100.toDuration(DurationUnit.SECONDS),
            200.toDuration(DurationUnit.SECONDS)
        )

        adHandler.createAdBreak(adBreak)

        verify(controller, never()).createAd(any(), any())
    }

    @Test
    fun createAdBreak_withAds_callsControllerCreateAdForEveryAd() {
        val adBreak = UplynkAdBreak(
            listOf(
                UplynkAd(
                    null,
                    listOf(),
                    "",
                    "",
                    mapOf(),
                    1f,
                    2f,
                    100.toDuration(DurationUnit.SECONDS)
                ),
                UplynkAd(
                    null,
                    listOf(),
                    "",
                    "",
                    mapOf(),
                    1f,
                    2f,
                    200.toDuration(DurationUnit.SECONDS)
                ),
                UplynkAd(
                    null,
                    listOf(),
                    "",
                    "",
                    mapOf(),
                    1f,
                    2f,
                    300.toDuration(DurationUnit.SECONDS)
                ),
            ), "", "", 400.toDuration(DurationUnit.SECONDS), 500.toDuration(DurationUnit.SECONDS)
        )

        adHandler.createAdBreak(adBreak)

        verify(controller, times(adBreak.ads.size)).createAd(any(), eq(mockAdBreak))
        verify(controller).createAd(
            eq(
                AdInit(
                    type = adBreak.type,
                    duration = 100,
                    customData = adBreak.ads[0]
                )
            ), eq(mockAdBreak)
        )
        verify(controller).createAd(
            eq(
                AdInit(
                    type = adBreak.type,
                    duration = 200,
                    customData = adBreak.ads[1]
                )
            ), eq(mockAdBreak)
        )
        verify(controller).createAd(
            eq(
                AdInit(
                    type = adBreak.type,
                    duration = 300,
                    customData = adBreak.ads[2]
                )
            ), eq(mockAdBreak)
        )
    }

    @Test
    fun onAdBegin_forUnknownAd_throwsAnException() {
        val ad =
            UplynkAd(null, listOf(), "", "", mapOf(), 1f, 2f, 100.toDuration(DurationUnit.SECONDS))

        assertThrows(java.lang.IllegalStateException::class.java) {
            adHandler.onAdBegin(ad)
        }
    }

    @Test
    fun onAdBegin_forCreatedAdBreak_callsBeginAd() {
        val uplynkAd =
            UplynkAd(null, listOf(), "", "", mapOf(), 1f, 2f, 100.toDuration(DurationUnit.SECONDS))
        val adBreak = UplynkAdBreak(
            listOf(uplynkAd),
            "",
            "",
            400.toDuration(DurationUnit.SECONDS),
            500.toDuration(DurationUnit.SECONDS)
        )
        adHandler.createAdBreak(adBreak)

        adHandler.onAdBegin(uplynkAd)

        verify(controller).beginAd(eq(mockAd))
    }

    @Test
    fun onAdEnd_forUnknownAd_throwsAnException() {
        val ad =
            UplynkAd(null, listOf(), "", "", mapOf(), 1f, 2f, 100.toDuration(DurationUnit.SECONDS))

        assertThrows(java.lang.IllegalStateException::class.java) {
            adHandler.onAdEnd(ad)
        }
    }

    @Test
    fun onAdEnd_forCreatedAdBreak_callsEndAd() {
        val uplynkAd =
            UplynkAd(null, listOf(), "", "", mapOf(), 1f, 2f, 100.toDuration(DurationUnit.SECONDS))
        val adBreak = UplynkAdBreak(
            listOf(uplynkAd),
            "",
            "",
            400.toDuration(DurationUnit.SECONDS),
            500.toDuration(DurationUnit.SECONDS)
        )
        adHandler.createAdBreak(adBreak)

        adHandler.onAdEnd(uplynkAd)

        verify(controller).endAd(eq(mockAd))
    }

    @Test
    fun onAdProgressUpdate_forCreatedAdBreak_callsUpdateAdProgress() {
        val uplynkAd =
            UplynkAd(null, listOf(), "", "", mapOf(), 1f, 2f, 100.toDuration(DurationUnit.SECONDS))
        val adBreak = UplynkAdBreak(
            listOf(uplynkAd),
            "",
            "",
            400.toDuration(DurationUnit.SECONDS),
            500.toDuration(DurationUnit.SECONDS)
        )
        adHandler.createAdBreak(adBreak)

        adHandler.onAdProgressUpdate(
            UplynkAdState(uplynkAd, AdState.STARTED),
            adBreak,
            450.toDuration(DurationUnit.SECONDS)
        )

        verify(controller).updateAdProgress(eq(mockAd), eq(0.5))
    }

    @Test
    fun onAdProgressUpdate_forUnknownAd_throwsAnException() {
        val ad =
            UplynkAd(null, listOf(), "", "", mapOf(), 1f, 2f, 100.toDuration(DurationUnit.SECONDS))
        val adBreak = UplynkAdBreak(
            listOf(ad),
            "",
            "",
            400.toDuration(DurationUnit.SECONDS),
            500.toDuration(DurationUnit.SECONDS)
        )


        assertThrows(java.lang.IllegalStateException::class.java) {
            adHandler.onAdProgressUpdate(
                UplynkAdState(ad, AdState.NOT_PLAYED),
                adBreak,
                Duration.ZERO
            )
        }
    }

}