package com.theoplayer.android.connector.uplynk.internal.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal const val CONNECT_TIMEOUT_IN_MS = 30000
internal const val READ_TIMEOUT_IN_MS = 30000

internal class HttpsConnection {
    suspend fun get(urlString: String): String = runInterruptible(context = Dispatchers.IO) {
        var connection: HttpsURLConnection? = null
        try {
            connection = (URL(urlString).openConnection() as HttpsURLConnection)
                .apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_IN_MS
                    readTimeout = READ_TIMEOUT_IN_MS
                    doInput = true
                }

            connection.connect()

            when (val responseCode: Int = connection.responseCode) {
                HttpsURLConnection.HTTP_OK ->
                    return@runInterruptible connection
                        .inputStream
                        .bufferedReader()
                        .use { it.readText() }

                else -> throw IOException("HTTP response $responseCode for URL: $urlString")
            }
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun retry(
        retry: Int = 3,
        initialDelay: Duration = 300.milliseconds,
        maxDelay: Duration = 5000.milliseconds,
        delayMultiplier: Double = 2.0,
        request: suspend HttpsConnection.() -> String
    ): String {
        var delayDuration = initialDelay
        for (remains in retry - 1 downTo 0) {
            try {
                return request()
            } catch (e: IOException) {
                if (remains == 0) throw IOException(e)
                delay(delayDuration)
                delayDuration = (delayDuration * delayMultiplier).coerceAtMost(maxDelay)
            }
        }
        throw IllegalStateException("Retry count should be bigger than 0 but it is $retry")
    }
}