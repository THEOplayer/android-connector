package com.theoplayer.android.connector.uplynk.internal.events

import com.theoplayer.android.api.event.EventType
import java.util.Date

abstract class UplynkEventImpl<E : UplynkEvent<E>> internal constructor(
    private val type: EventType<E>,
    private val date: Date
) : UplynkEvent<E> {

    override fun getDate() = date

    override fun getType() = type

    override fun toString(): String {
        return "UplynkEvent{" +
                "type=" + type +
                ", date=" + date +
                '}'
    }
}