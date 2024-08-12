package com.theoplayer.android.connector.uplynk.internal.events

import com.theoplayer.android.connector.uplynk.events.UplynkEventTypes
import com.theoplayer.android.connector.uplynk.events.UplynkPreplayErrorResponseEvent
import java.util.Date

internal class UplynkPreplayErrorResponseEventImpl(
    date: Date,
    private val body: String,
    private val exception: Exception?
) :
    UplynkEventImpl<UplynkPreplayErrorResponseEvent>(UplynkEventTypes.PREPLAY_RESPONSE_ERROR, date),
    UplynkPreplayErrorResponseEvent {
    override fun getException() = exception

    override fun getBody() = body
}