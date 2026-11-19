package com.theoplayer.android.connector.analytics.conviva

import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class TestEventDispatcherTest {

    private fun dispatcher() = TestEventDispatcher<PlayerEvent<*>>()

    @Test
    fun dispatch_deliversEventToMatchingListenerOnly() {
        val dispatcher = dispatcher()
        var received = 0
        dispatcher.addEventListener(PlayerEventTypes.PLAY, EventListener<PlayEvent> { received++ })
        dispatcher.dispatchEvent(PlayerEventTypes.PLAY, mock())
        dispatcher.dispatchEvent(PlayerEventTypes.PAUSE, mock<PauseEvent>())
        assertEquals(1, received)
    }

    @Test
    fun dispatch_deliversToAllListenersInRegistrationOrder() {
        val dispatcher = dispatcher()
        val order = mutableListOf<Int>()
        dispatcher.addEventListener(PlayerEventTypes.PLAY, EventListener<PlayEvent> { order.add(1) })
        dispatcher.addEventListener(PlayerEventTypes.PLAY, EventListener<PlayEvent> { order.add(2) })
        dispatcher.dispatchEvent(PlayerEventTypes.PLAY, mock())
        assertEquals(listOf(1, 2), order)
    }

    @Test
    fun dispatch_skipsRemovedListener() {
        val dispatcher = dispatcher()
        var received = 0
        var receivedByRemoved = 0
        val removed = EventListener<PlayEvent> { receivedByRemoved++ }
        dispatcher.addEventListener(PlayerEventTypes.PLAY, EventListener<PlayEvent> { received++ })
        dispatcher.addEventListener(PlayerEventTypes.PLAY, removed)
        dispatcher.removeEventListener(PlayerEventTypes.PLAY, removed)
        dispatcher.dispatchEvent(PlayerEventTypes.PLAY, mock())
        assertEquals(1, received)
        assertEquals(0, receivedByRemoved)
        assertTrue(dispatcher.hasListeners(PlayerEventTypes.PLAY))
    }

    @Test
    fun remove_neverAddedListener_isNoOp() {
        val dispatcher = dispatcher()
        dispatcher.removeEventListener(PlayerEventTypes.PLAY, EventListener<PlayEvent> { })
        assertFalse(dispatcher.hasListeners(PlayerEventTypes.PLAY))
    }

    @Test
    fun dispatch_listenerRemovingItself_doesNotBreakDispatch() {
        val dispatcher = dispatcher()
        var received = 0
        var receivedBySelfRemoving = 0
        val selfRemoving = object : EventListener<PlayEvent> {
            override fun handleEvent(event: PlayEvent) {
                receivedBySelfRemoving++
                dispatcher.removeEventListener(PlayerEventTypes.PLAY, this)
            }
        }
        dispatcher.addEventListener(PlayerEventTypes.PLAY, selfRemoving)
        dispatcher.addEventListener(PlayerEventTypes.PLAY, EventListener { received++ })
        dispatcher.dispatchEvent(PlayerEventTypes.PLAY, mock())
        dispatcher.dispatchEvent(PlayerEventTypes.PLAY, mock())
        assertEquals(2, received)
        assertEquals(1, receivedBySelfRemoving)
        assertTrue(dispatcher.hasListeners(PlayerEventTypes.PLAY))
    }
}
