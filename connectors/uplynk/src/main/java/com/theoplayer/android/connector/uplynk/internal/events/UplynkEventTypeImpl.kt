package com.theoplayer.android.connector.uplynk.internal.events

import com.theoplayer.android.api.event.EventType
import com.theoplayer.android.connector.uplynk.events.UplynkEvent

internal class UplynkEventTypeImpl<E : UplynkEvent<*>>(private val name: String) : EventType<E> {
    override fun getName() = this.name

    override fun toString(): String {
        return this.name
    }
}