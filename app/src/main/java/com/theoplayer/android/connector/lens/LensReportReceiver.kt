package com.theoplayer.android.connector.lens

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/** One report Lens sent, as the in-app receiver got it. */
data class LensReport(val receivedAt: Long, val json: JSONObject)

/**
 * A Lens JSON destination inside the app, on the loopback interface: shows what Lens sends without a server elsewhere.
 * Lens only sends to HTTP destinations, so this is a minimal HTTP endpoint that accepts any POST.
 */
class LensReportReceiver {
    private val socket = ServerSocket(0, 16, InetAddress.getLoopbackAddress())
    private val mainHandler = Handler(Looper.getMainLooper())

    /** The reports received so far, newest first; read and cleared on the main thread. */
    val reports = mutableStateListOf<LensReport>()

    val url = "http://127.0.0.1:${socket.localPort}/lens"

    init {
        thread(name = "LensReportReceiver", isDaemon = true) {
            while (!socket.isClosed) {
                try {
                    socket.accept().use(::handle)
                } catch (e: Exception) {
                    if (!socket.isClosed) Log.w(TAG, "Lens receiver failed to handle a request", e)
                }
            }
        }
    }

    fun close() {
        socket.close()
    }

    private fun handle(client: Socket) {
        val input = BufferedInputStream(client.getInputStream())
        var contentLength = 0
        while (true) {
            val line = input.readLine() ?: return
            if (line.isEmpty()) break
            val (name, value) = line.split(":", limit = 2).takeIf { it.size == 2 } ?: continue
            if (name.trim().equals("Content-Length", ignoreCase = true)) contentLength = value.trim().toInt()
        }
        val body = ByteArray(contentLength).also { input.readFully(it) }.decodeToString()
        client.getOutputStream().write("HTTP/1.1 204 No Content\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
        val receivedAt = System.currentTimeMillis()
        val sent = JSONObject(body).optJSONArray("reports") ?: return
        val received = (0 until sent.length()).map { LensReport(receivedAt, sent.getJSONObject(it)) }
        mainHandler.post { reports.addAll(0, received.reversed()) }
    }

    private fun InputStream.readLine(): String? {
        val line = StringBuilder()
        while (true) {
            val byte = read()
            if (byte == -1) return if (line.isEmpty()) null else line.toString()
            if (byte == '\n'.code) return line.toString().trimEnd('\r')
            line.append(byte.toChar())
        }
    }

    private fun InputStream.readFully(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read == -1) break
            offset += read
        }
    }

    private companion object {
        const val TAG = "LensReportReceiver"
    }
}
