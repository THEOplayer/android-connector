package com.theoplayer.android.connector

import androidx.annotation.WorkerThread
import java.net.HttpURLConnection
import java.net.URL

@WorkerThread
fun requestStreams(url: String): String {
    var connection: HttpURLConnection? = null
    try {
        connection = URL(url).openConnection() as HttpURLConnection
        connection.connect()
        return connection.inputStream.readBytes().decodeToString()
    } finally {
        connection?.disconnect()
    }
}
