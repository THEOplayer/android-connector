package com.theoplayer.android.connector.uplynk.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal const val CONNECT_TIMEOUT_IN_MS = 30000
internal const val READ_TIMEOUT_IN_MS = 30000

internal class HttpsConnection {
    suspend fun get(urlString: String): String = runInterruptible(context = Dispatchers.IO) {
            var result: String
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

                val responseCode: Int = connection.getResponseCode()
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    val reader = InputStreamReader(connection.inputStream)
                    result = reader
                        .buffered()
                        .readText()
                    reader.close()
                } else {
                    result = "Error: $responseCode"
                }
            } catch (e: Exception) {
                result = e.message ?: ""
            } finally {
                connection?.disconnect()
            }

            result
        }


}
