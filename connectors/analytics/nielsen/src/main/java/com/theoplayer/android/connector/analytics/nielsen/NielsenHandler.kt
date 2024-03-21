package com.theoplayer.android.connector.analytics.nielsen

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.nielsen.app.sdk.AppLaunchMeasurementManager
import com.nielsen.app.sdk.AppSdk
import com.theoplayer.android.api.ads.ima.GoogleImaAdEvent
import com.theoplayer.android.api.ads.ima.GoogleImaAdEventType
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.*
import com.theoplayer.android.api.event.track.texttrack.EnterCueEvent
import com.theoplayer.android.api.event.track.texttrack.TextTrackEventTypes
import com.theoplayer.android.api.event.track.texttrack.list.AddTrackEvent
import com.theoplayer.android.api.event.track.texttrack.list.TextTrackListEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.track.texttrack.TextTrackMode
import com.theoplayer.android.api.player.track.texttrack.TextTrackType
import org.json.JSONArray
import org.json.JSONObject

private const val PROP_APP_ID = "appid"
private const val PROP_DEBUG = "nol_devDebug"
private const val PROP_CHANNEL_NAME = "channelname"
private const val PROP_TYPE = "type"
private const val PROP_ADMODEL = "adModel"
private const val PROP_ASSET_ID = "assetid"
private const val PROP_PREROLL = "preroll"
private const val PROP_MIDROLL = "midroll"
private const val PROP_POSTROLL = "postroll"

/**
 * NielsenHandler
 *
 * @param appContext  Application context.
 * @param player      THEOplayer instance.
 * @param appId       Unique Nielsen ID for the application. The ID is a GUID data type.
 * @param debug       Enables Nielsen console logging. Only required for testing
 */
class NielsenHandler(
    appContext: Context,
    private val player: Player,
    appId: String,
    debug: Boolean = false,
) {
    private val onPlay: EventListener<PlayEvent>
    private val onPause: EventListener<PauseEvent>
    private val onEnded: EventListener<EndedEvent>
    private val onSourceChange: EventListener<SourceChangeEvent>
    private val onDurationChange: EventListener<DurationChangeEvent>
    private val onLoadedMetadata: EventListener<LoadedMetadataEvent>
    private val onAddTrack: EventListener<AddTrackEvent>
    private val onAdBegin: EventListener<GoogleImaAdEvent>
    private val onAdEnd: EventListener<GoogleImaAdEvent>
    private val onCueEnterId3: EventListener<EnterCueEvent>
    private val onCueEnterEmsg: EventListener<EnterCueEvent>

    private var lastPosition: Long = -1
    private var appSdk: AppSdk? = null

    private var sessionInProgress: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lifecycleObserver: LifecycleObserver

    init {
        // Initialize SDK
        val appInfo = JSONObject().apply {
            put(PROP_APP_ID, appId)
            if (debug) {
                put(PROP_DEBUG, "DEBUG")
            }
        }
        appSdk = AppSdk(appContext, appInfo) { _, _, _ -> }

        onPlay = EventListener<PlayEvent> {
            maybeSendPlayEvent()
        }
        onPause = EventListener<PauseEvent> {
            appSdk?.stop()
        }
        onDurationChange = EventListener<DurationChangeEvent> {
            maybeSendPlayEvent()
        }
        onEnded = EventListener<EndedEvent> {
            endSession()
        }
        onSourceChange = EventListener<SourceChangeEvent> {
            endSession()
        }
        onLoadedMetadata = EventListener<LoadedMetadataEvent> {
            // contentMetadataObject contains the JSON metadata for the content being played
            appSdk?.loadMetadata(buildMetadata())
        }
        onCueEnterId3 = EventListener<EnterCueEvent> { event ->
            event.cue.content?.optJSONObject("content")?.let { cueContent ->
                handleNielsenId3Payload(cueContent) {
                    appSdk?.sendID3(it)
                }
            }
        }
        onCueEnterEmsg = EventListener<EnterCueEvent> { event ->
            event.cue.content?.optJSONArray("content")?.let { cueContent ->
                handleNielsenEmsgPayload(cueContent) {
                    appSdk?.sendID3(it)
                }
            }
        }
        onAddTrack = EventListener<AddTrackEvent> { event ->
            if (event.track.type == TextTrackType.ID3 || event.track.type == TextTrackType.EMSG) {
                // Make sure we listen for cues
                if (event.track.mode == TextTrackMode.DISABLED) {
                    event.track.mode = TextTrackMode.HIDDEN
                }
                event.track.addEventListener(
                    TextTrackEventTypes.ENTERCUE,
                    if (event.track.type == TextTrackType.ID3) onCueEnterId3 else onCueEnterEmsg
                )
            }
        }
        onAdBegin = EventListener<GoogleImaAdEvent> { event ->
            val ad = event.ad
            if (ad?.type == "linear") {
                val timeOffset = ad.adBreak?.timeOffset ?: 0
                appSdk?.stop()
                appSdk?.loadMetadata(buildMetadata().apply {
                    put(
                        PROP_TYPE, when {
                            timeOffset == 0 -> PROP_PREROLL
                            timeOffset > 0 -> PROP_MIDROLL
                            else -> PROP_POSTROLL
                        }
                    )
                    put(PROP_ASSET_ID, ad.id)
                })
            }
        }
        onAdEnd = EventListener<GoogleImaAdEvent> { event ->
            val ad = event.ad
            if (ad?.type == "linear") {
                appSdk?.stop()
            }
        }

        // Observe app switches between background and foreground
        lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    @Suppress("DEPRECATION")
                    AppLaunchMeasurementManager.appInBackground(appContext)
                }

                Lifecycle.Event.ON_RESUME -> {
                    @Suppress("DEPRECATION")
                    AppLaunchMeasurementManager.appInForeground(appContext)
                }

                else -> {/*ignore*/
                }
            }
        }

        addEventListeners()
    }

    fun updateMetadata(metadata: Map<String, Any>) {
        appSdk?.loadMetadata(buildMetadata(metadata))
    }

    fun destroy() {
        removeEventListeners()
        endSession()
    }

    private fun addEventListeners() {
        player.addEventListener(PlayerEventTypes.PLAY, onPlay)
        player.addEventListener(PlayerEventTypes.PAUSE, onPause)
        player.addEventListener(PlayerEventTypes.DURATIONCHANGE, onDurationChange)
        player.addEventListener(PlayerEventTypes.ENDED, onEnded)
        player.addEventListener(PlayerEventTypes.SOURCECHANGE, onSourceChange)
        player.addEventListener(PlayerEventTypes.LOADEDMETADATA, onLoadedMetadata)
        player.textTracks.addEventListener(TextTrackListEventTypes.ADDTRACK, onAddTrack)
        player.ads.addEventListener(GoogleImaAdEventType.STARTED, onAdBegin)
        player.ads.addEventListener(GoogleImaAdEventType.COMPLETED, onAdEnd)
        mainHandler.post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        }
    }

    private fun removeEventListeners() {
        player.removeEventListener(PlayerEventTypes.PLAY, onPlay)
        player.removeEventListener(PlayerEventTypes.PAUSE, onPause)
        player.removeEventListener(PlayerEventTypes.DURATIONCHANGE, onDurationChange)
        player.removeEventListener(PlayerEventTypes.ENDED, onEnded)
        player.removeEventListener(PlayerEventTypes.SOURCECHANGE, onSourceChange)
        player.removeEventListener(PlayerEventTypes.LOADEDMETADATA, onLoadedMetadata)
        player.textTracks.removeEventListener(TextTrackListEventTypes.ADDTRACK, onAddTrack)
        player.ads.removeEventListener(GoogleImaAdEventType.STARTED, onAdBegin)
        player.ads.removeEventListener(GoogleImaAdEventType.COMPLETED, onAdEnd)
        mainHandler.post {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        }
    }

    private fun maybeSendPlayEvent() {
        if (!sessionInProgress && !player.duration.isNaN()) {
            sessionInProgress = true

            // stream starts
            appSdk?.play(JSONObject().apply {
                put(PROP_CHANNEL_NAME, player.src)
            })
        }
    }

    private fun endSession() {
        if (sessionInProgress) {
            lastPosition = -1
            sessionInProgress = false
            appSdk?.end()
        }
    }

    private fun buildMetadata(metadata: Map<String, Any> = mapOf()): JSONObject {
        return JSONObject(metadata).apply {
            put(PROP_TYPE, "content")
            put(PROP_ADMODEL, "1")
        }
    }
}

private fun handleNielsenId3Payload(cueContent: JSONObject, handle: (result: String) -> Unit) {
    cueContent.optString("ownerIdentifier").let {
        if (it.contains("www.nielsen.com")) {
            handle(it)
        }
    }
}

private fun handleNielsenEmsgPayload(cueContent: JSONArray, handle: (result: String) -> Unit) {
    // Convert to String
    val cueContentText =
        cueContent.let { json -> ByteArray(json.length()) { json.getInt(it).toByte() } }
            .toString(Charsets.UTF_8)

    // Retain only nielsen tags
    if (cueContentText.startsWith("type=nielsen_tag")) {
        // Decode payload
        val base64 =
            cueContentText.substring(cueContentText.indexOf("payload=") + "payload=".length)
        val decoded = Base64.decode(base64, Base64.DEFAULT).toString(Charsets.UTF_8)
        // Sanitise content
        val cleaned = decoded.filter { it.code in 32..126 }
        handle(cleaned)
    }
}
