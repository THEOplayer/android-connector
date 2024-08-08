package com.theoplayer.android.connector.uplynk.internal.events

import com.theoplayer.android.connector.uplynk.network.PreplayResponse
import java.util.Date

class UplynkPreplayResponseEventImpl(
    date: Date,
    private val response: PreplayResponse
) :
    UplynkEventImpl<UplynkPreplayResponseEvent>(UplynkEventTypes.PREPLAY_RESPONSE, date),
    UplynkPreplayResponseEvent {
        override fun getResponse(): PreplayResponse = response
    }