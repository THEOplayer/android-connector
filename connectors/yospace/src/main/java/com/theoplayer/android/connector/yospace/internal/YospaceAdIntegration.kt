package com.theoplayer.android.connector.yospace.internal

import android.util.Log
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.connector.yospace.TAG
import com.theoplayer.android.connector.yospace.YospaceListener
import com.theoplayer.android.connector.yospace.YospaceSsaiDescription
import com.theoplayer.android.connector.yospace.YospaceStreamType
import com.yospace.admanagement.Session
import com.yospace.admanagement.SessionDVRLive
import com.yospace.admanagement.SessionLive
import com.yospace.admanagement.SessionVOD
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class YospaceAdIntegration(
    private val controller: ServerSideAdIntegrationController,
    private val listener: YospaceListener
) : ServerSideAdIntegrationHandler {
    private var session: Session? = null

    override suspend fun setSource(source: SourceDescription): SourceDescription {
        val yospaceSource = source.sources.find { it.ssai is YospaceSsaiDescription } ?: return source
        val ssaiDescription = yospaceSource.ssai as? YospaceSsaiDescription ?: return source

        // Create the Yospace session
        val src = yospaceSource.src
        val session = when (ssaiDescription.streamType) {
            YospaceStreamType.LIVE -> createSessionLive(src, ssaiDescription.sessionProperties)

            YospaceStreamType.NONLINEAR,
            YospaceStreamType.LIVEPAUSE -> createSessionDVRLive(src, ssaiDescription.sessionProperties)

            YospaceStreamType.VOD -> createSessionVOD(src, ssaiDescription.sessionProperties)
        }
        this.session = session

        // Load the source
        when (session.sessionState) {
            Session.SessionState.INITIALISED,
            Session.SessionState.NO_ANALYTICS -> {
                // Notify listener
                listener.onSessionAvailable()
                // Replace source with playback URL
                val newSource = source.replaceSources(source.sources.toMutableList().apply {
                    remove(yospaceSource)
                    add(0, yospaceSource.replaceSrc(session.playbackUrl))
                })
                return newSource
            }

            Session.SessionState.FAILED -> {
                val resultCode = session.resultCode
                session.shutdown()
                throw Exception(getSessionErrorMessage(resultCode))
            }

            Session.SessionState.SHUTDOWN -> {
                // Already shutdown, ignore
            }

            else -> {
                Log.d(TAG, "Unexpected Yospace session state: ${session.sessionState}")
                session.shutdown()
            }
        }
        return source
    }

    override suspend fun resetSource() {
        session?.shutdown()
        session = null
    }
}

private suspend fun createSessionLive(url: String, properties: Session.SessionProperties?) = suspendCoroutine { continuation ->
    SessionLive.create(url, properties) { event -> continuation.resume(event.payload) }
}

private suspend fun createSessionDVRLive(url: String, properties: Session.SessionProperties?) = suspendCoroutine { continuation ->
    SessionDVRLive.create(url, properties) { event -> continuation.resume(event.payload) }
}

private suspend fun createSessionVOD(url: String, properties: Session.SessionProperties?) = suspendCoroutine { continuation ->
    SessionVOD.create(url, properties) { event -> continuation.resume(event.payload) }
}

private fun getSessionErrorMessage(resultCode: Int): String {
    val message = when (resultCode) {
        YospaceSessionResultCode.MALFORMED_URL -> "The stream URL is not correctly formatted"
        YospaceSessionResultCode.CONNECTION_ERROR -> "Connection error"
        YospaceSessionResultCode.CONNECTION_TIMEOUT -> "Connection timeout"
        else -> "Session could not be initialised"
    }
    return "Yospace: $message (code = $resultCode)"
}