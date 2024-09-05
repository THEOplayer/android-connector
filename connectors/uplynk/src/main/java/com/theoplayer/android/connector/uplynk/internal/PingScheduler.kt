package com.theoplayer.android.connector.uplynk.internal

import com.theoplayer.android.connector.uplynk.internal.network.UplynkApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class PingScheduler(
    private val uplynkApi: UplynkApi,
    private val uplynkDescriptionConverter: UplynkSsaiDescriptionConverter,
    private val prefix: String,
    private val sessionId: String,
    private val eventDispatcher: UplynkEventDispatcher,
    private val adScheduler: UplynkAdScheduler
) {
    private val NEGATIVE_TIME = (-1).toDuration(DurationUnit.SECONDS)

    private var nextRequestTime: Duration = NEGATIVE_TIME
    private var seekStart: Duration = NEGATIVE_TIME
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun onTimeUpdate(time: Duration) {
        if (nextRequestTime.isPositive() && time > nextRequestTime) {
            nextRequestTime = NEGATIVE_TIME
            performPing(uplynkDescriptionConverter.buildPingUrl(prefix, sessionId, time))
        }
    }

    fun onStart() =
        performPing(uplynkDescriptionConverter.buildStartPingUrl(prefix, sessionId, Duration.ZERO))


    fun onSeeking(time: Duration) {
        if (seekStart.isNegative()) {
            seekStart = time
        }
    }

    fun onSeeked(time: Duration) {
        performPing(
            uplynkDescriptionConverter.buildSeekedPingUrl(
                prefix,
                sessionId,
                time,
                seekStart
            )
        )
        seekStart = NEGATIVE_TIME
    }

    fun destroy() {
        job.cancel()
    }

    private fun performPing(url: String) = scope.launch {
        val pingResponse = uplynkApi.ping(url)
        nextRequestTime = pingResponse.nextTime
        eventDispatcher.dispatchPingEvent(pingResponse)
        if (pingResponse.ads != null) {
            adScheduler.add(pingResponse.ads)
        }
    }
}
