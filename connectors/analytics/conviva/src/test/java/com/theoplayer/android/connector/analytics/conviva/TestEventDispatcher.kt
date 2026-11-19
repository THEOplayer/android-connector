package com.theoplayer.android.connector.analytics.conviva

import com.theoplayer.android.api.event.Event
import com.theoplayer.android.api.event.EventDispatcher
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.EventType

/**
 * An [EventDispatcher] implementation for unit tests: keeps the listeners it is
 * given so tests can dispatch events to them.
 */
class TestEventDispatcher<E : Event<*>> : EventDispatcher<E> {
    private val listeners = mutableMapOf<EventType<*>, MutableList<EventListener<*>>>()

    override fun <T : E> addEventListener(eventType: EventType<T>, eventListener: EventListener<in T>) {
        listeners.getOrPut(eventType) { mutableListOf() }.add(eventListener)
    }

    override fun <T : E> removeEventListener(eventType: EventType<T>, eventListener: EventListener<in T>) {
        listeners[eventType]?.remove(eventListener)
    }

    // Iterate over a copy so a listener may remove itself during dispatch.
    @Suppress("UNCHECKED_CAST")
    fun <T : E> dispatchEvent(eventType: EventType<T>, event: T) {
        listeners[eventType]?.toList()?.forEach { (it as EventListener<T>).handleEvent(event) }
    }

    fun hasListeners(eventType: EventType<*>): Boolean = !listeners[eventType].isNullOrEmpty()
}
