package com.theoplayer.android.connector.analytics.lens

import android.os.Looper
import com.dolby.optiview.lens.Lens
import com.dolby.optiview.lens.LensBackend
import com.dolby.optiview.lens.LensConfigureOptions
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.connector.analytics.lens.internal.TestTHEOplayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class LensConnectorTest {
    @Before
    fun setUp() {
        Lens.configure(LensConfigureOptions(LensBackend.DISABLED))
    }

    /** Joins Lens and lets it subscribe to the player, which it does through the player's main-thread executor. */
    private fun connect(theoplayer: TestTHEOplayer): LensConnector =
        LensConnector(theoplayer.view).also { shadowOf(Looper.getMainLooper()).idle() }

    private fun source(url: String) = SourceDescription(listOf(TypedSource(url)))

    @Test
    fun cmcdLinkedSessionId_followsTheLensSessionUntilDestroy() {
        val theoplayer = TestTHEOplayer()
        val connector = connect(theoplayer)
        assertNull(theoplayer.cmcd.linkedSessionId)

        theoplayer.setSource(source("https://cdn.example.com/a.m3u8"))
        val first = connector.handle.session?.sessionId
        assertNotNull(first)
        assertEquals(first, theoplayer.cmcd.linkedSessionId)

        theoplayer.setSource(source("https://cdn.example.com/b.m3u8"))
        val second = connector.handle.session?.sessionId
        assertNotEquals(first, second)
        assertEquals(second, theoplayer.cmcd.linkedSessionId)

        connector.destroy()
        assertNull(theoplayer.cmcd.linkedSessionId)
    }

    @Test
    fun aPlayerWithoutCmcd_isStillReported() {
        val theoplayer = TestTHEOplayer(withCmcd = false)
        val connector = connect(theoplayer)

        theoplayer.setSource(source("https://cdn.example.com/a.m3u8"))

        assertNotNull(connector.handle.session)
        connector.destroy()
    }
}
