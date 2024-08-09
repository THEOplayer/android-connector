package com.theoplayer.android.connector.uplynk.internal.events

import java.util.Date

class UplynkPreplayErrorResponseEventImpl(
    date: Date,
    private val body: String,
    private val exception: Exception?
) :
    UplynkEventImpl<UplynkPreplayErrorResponseEvent>(UplynkEventTypes.PREPLAY_RESPONSE_ERROR, date),
    UplynkPreplayErrorResponseEvent {
    override fun getException() = exception

    override fun getBody() = body
}