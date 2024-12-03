package com.theoplayer.android.connector.analytics.conviva.utils

import com.theoplayer.android.api.error.THEOplayerException
import com.theoplayer.android.api.network.http.HTTPInterceptor
import com.theoplayer.android.api.network.http.InterceptableHTTPResponse
import com.theoplayer.android.api.player.Player

/**
 * ErrorReportBuilder provides extra error details that can be send when reporting an error.
 */
class ErrorReportBuilder(private val maxHTTPResponses: Int = 10) : HTTPInterceptor {
    private val _responses = LinkedHashMap<String, String>(maxHTTPResponses, 0.75f, true)
    private val _report = mutableMapOf<String, String>()

    override suspend fun onResponse(response: InterceptableHTTPResponse) {
        // Keep info on the last X HTTP responses
        if (_responses.size == maxHTTPResponses) {
            _responses.remove(_responses.keys.first())
        }
        // Create an entry with the path & http response status
        var respDesc =
            "${response.url.path},status: ${response.status} ${response.statusText},"
        // Add interesting header values
        respDesc += listOf("content-length").joinToString(",") { key ->
            "$key: ${getHeaderValue(response.headers, key)}"
        }
        _responses[System.currentTimeMillis().toString()] = respDesc
        super.onResponse(response)
    }

    fun withPlayerBuffer(player: Player) {
        _report["buffered"] = bufferedToString(player.buffered)
    }

    fun withErrorDetails(exception: THEOplayerException) {
        val errorDetails = flattenErrorObject(exception)
        if (errorDetails.isNotEmpty()) {
            _report += errorDetails
        }
    }

    fun build(): Map<String, String> {
        // Merge report and a list of the latest http responses in separate entries as event
        // payloads are truncated in the Pulse dashboard.
        return _report + _responses
    }
}

fun getHeaderValue(headers: Map<String, String>, key: String): String {
    return headers.keys.firstOrNull { it.equals(key, ignoreCase = true) }
        ?.let { headers[it] } ?: "N/A"
}

fun flattenErrorObject(error: THEOplayerException): Map<String, String> {
    return mapOf(
        "code" to error.code.name,
        "category" to error.category.name,
        "stack" to (error.stackTraceToString()),
        "cause.stack" to (error.cause?.stackTraceToString() ?: ""),
        "cause.message" to (error.cause?.message ?: "")
    ).filterValues { it != "" } // Remove entries with empty values
}
