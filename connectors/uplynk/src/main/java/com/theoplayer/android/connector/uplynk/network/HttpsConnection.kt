package com.theoplayer.android.connector.uplynk.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection


internal class HttpsConnection {
    suspend fun get(urlString: String): String = runInterruptible(context = Dispatchers.IO) {
            var result: String
            var urlConnection: HttpsURLConnection? = null
            try {
                val url = URL(urlString)
                urlConnection = url.openConnection() as HttpsURLConnection
                urlConnection.setRequestMethod("GET")
                urlConnection.setReadTimeout(10000)
                urlConnection.setConnectTimeout(15000)
                urlConnection.setDoInput(true)

                urlConnection.connect()

                val responseCode: Int = urlConnection.getResponseCode()
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(urlConnection.getInputStream()))
                    var inputLine: String?
                    val response = StringBuilder()

                    while ((reader.readLine().also { inputLine = it }) != null) {
                        response.append(inputLine)
                    }
                    reader.close()
                    result = response.toString()
                } else {
                    result = "Error: $responseCode"
                }
            } catch (e: Exception) {
                result = e.message ?: ""
            } finally {
                urlConnection?.disconnect()
            }

            result
        }


}
