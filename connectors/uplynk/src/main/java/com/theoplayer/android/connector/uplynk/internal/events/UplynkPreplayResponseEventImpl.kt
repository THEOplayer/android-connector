package com.theoplayer.android.connector.uplynk.internal.events

import com.theoplayer.android.api.event.EventType
import com.theoplayer.android.connector.uplynk.network.PreplayResponse
import java.util.Date

class UplynkPreplayResponseEventImpl(
    type: EventType<UplynkPreplayResponseEvent>,
    date: Date,
    val response: PreplayResponse
) :
    UplynkEventImpl<UplynkPreplayResponseEvent>(type, date)