/**
 * The QueuedConvivaEvent extension allows reporting events before a session has started.
 */
package com.theoplayer.android.connector.analytics.conviva.extension

import com.conviva.sdk.ConvivaVideoAnalytics
import java.util.WeakHashMap

data class QueuedConvivaEvent(
    val eventType: String,
    val eventDetails: Map<String, Any>
)

private val queuedEventsMap =
    WeakHashMap<ConvivaVideoAnalytics, MutableList<QueuedConvivaEvent>>()

val ConvivaVideoAnalytics.queuedEvents: MutableList<QueuedConvivaEvent>
    get() = queuedEventsMap.getOrPut(this) { mutableListOf() }

fun ConvivaVideoAnalytics.queueOrReportPlaybackEvent(
    eventType: String,
    eventDetails: Map<String, Any>
) {
    // Keep the event in case the session has not started yet.
    if (this.sessionId == -1 || this.sessionId == -2) {
        queuedEvents.add(QueuedConvivaEvent(eventType, eventDetails))
    } else {
        reportPlaybackEvent(eventType, eventDetails)
    }
}

fun ConvivaVideoAnalytics.reportQueuedPlaybackEvents() {
    queuedEvents.forEach { event ->
        reportPlaybackEvent(event.eventType, event.eventDetails)
    }
    queuedEvents.clear()
}
