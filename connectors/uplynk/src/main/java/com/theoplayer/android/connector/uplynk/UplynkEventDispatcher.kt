package com.theoplayer.android.connector.uplynk

import android.os.Handler
import android.os.Looper
import com.theoplayer.android.api.event.EventDispatcher
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.EventType
import com.theoplayer.android.connector.uplynk.internal.events.UplynkEvent


class UplynkEventDispatcher : EventDispatcher<UplynkEvent<*>> {
    private val eventMap: HashMap<EventType<*>, ArrayList<EventListener<in UplynkEvent<*>>>> = HashMap()
    private val handler = Handler(Looper.getMainLooper())
    override fun <E : UplynkEvent<*>> addEventListener(
        type: EventType<E>,
        listener: EventListener<in E>
    ) {
       eventMap.getOrPut(type) { ArrayList(1) }.add(listener as EventListener<in UplynkEvent<*>>)
    }

    override fun <E : UplynkEvent<*>> removeEventListener(
        type: EventType<E>,
        listener: EventListener<in E>
    ) {
        eventMap[type]?.remove(listener)
    }

    fun dispatchEvent(event: UplynkEvent<*>) = handler.post {
        eventMap[event.type]?.forEach { it.handleEvent(event) }
    }
}
