package com.theoplayer.android.connector.yospace.internal

import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.track.texttrack.CueChangeEvent
import com.theoplayer.android.api.event.track.texttrack.TextTrackEventTypes
import com.theoplayer.android.api.event.track.texttrack.list.AddTrackEvent
import com.theoplayer.android.api.event.track.texttrack.list.RemoveTrackEvent
import com.theoplayer.android.api.event.track.texttrack.list.TextTrackListEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.track.texttrack.TextTrack
import com.theoplayer.android.api.player.track.texttrack.TextTrackType
import com.theoplayer.android.api.player.track.texttrack.cue.TextTrackCue
import com.yospace.admanagement.PlaybackEventHandler
import com.yospace.admanagement.TimedMetadata
import org.json.JSONArray

private const val YOSPACE_EMSG_SCHEME_ID_URI = "urn:yospace:a:id3:2016";

class TimedMetadataHandler(
    private val player: Player,
    private val handler: PlaybackEventHandler
) {
    private fun handleTrackAdded(track: TextTrack) {
        if (track.kind != "metadata") {
            return
        }
        when (track.type) {
            TextTrackType.ID3 -> {
                handleId3CueChange(track)
                track.addEventListener(TextTrackEventTypes.CUECHANGE, onId3CueChange)
            }

            TextTrackType.EMSG -> {
                handleEmsgCueChange(track)
                track.addEventListener(TextTrackEventTypes.CUECHANGE, onEmsgCueChange)
            }

            else -> return
        }
    }

    private fun handleTrackRemoved(track: TextTrack) {
        track.removeEventListener(TextTrackEventTypes.CUECHANGE, onId3CueChange)
        track.removeEventListener(TextTrackEventTypes.CUECHANGE, onEmsgCueChange)
    }

    private fun handleId3CueChange(track: TextTrack) {
        val activeCues = (track.activeCues ?: return).filter(::isYospaceId3Cue)
        var startTime = (activeCues.firstOrNull() ?: return).startTime
        var report = YospaceReport()
        for (cue in activeCues) {
            // cue.content.content is an ID3 frame encoded as a JSON object.
            // See `ID3Yospace` in THEOplayer Web SDK for the full type definition.
            val id3 = cue.content!!.getJSONObject("content")
            report.update(id3.getString("id"), id3.optString("text"))
            if (cue.startTime != startTime) {
                finishReport(report, cue.startTime)
                report = YospaceReport()
                startTime = cue.startTime
            }
        }
        finishReport(report, startTime)
    }

    private fun handleEmsgCueChange(track: TextTrack) {
        val activeCues = (track.activeCues ?: return).filter(::isYospaceEmsgCue)
        for (cue in activeCues) {
            val report = YospaceReport()
            // cue.content.content is a byte array of a UTF-8 encoded string
            // holding comma-separated `key=value` pairs.
            val text = jsonArrayToByteArray(cue.content!!.getJSONArray("content")).toString(Charsets.UTF_8)
            for (pair in text.splitToSequence(',')) {
                val (key, value) = pair.split('=', limit = 2)
                report.update(key, value)
                finishReport(report, cue.startTime)
            }
        }
    }

    private fun finishReport(report: YospaceReport, startTime: Double) {
        report.finish((startTime * 1000).toLong())?.let { handler.onTimedMetadata(it) }
    }

    private val onAddTrack = EventListener<AddTrackEvent> { handleTrackAdded(it.track) }
    private val onRemoveTrack = EventListener<RemoveTrackEvent> { handleTrackRemoved(it.track) }
    private val onId3CueChange = EventListener<CueChangeEvent> { handleId3CueChange(it.textTrack) }
    private val onEmsgCueChange = EventListener<CueChangeEvent> { handleEmsgCueChange(it.textTrack) }

    init {
        player.textTracks.forEach { handleTrackAdded(it) }
        player.textTracks.addEventListener(TextTrackListEventTypes.ADDTRACK, onAddTrack)
        player.textTracks.addEventListener(TextTrackListEventTypes.REMOVETRACK, onRemoveTrack)
    }

    fun destroy() {
        player.textTracks.forEach { handleTrackRemoved(it) }
        player.textTracks.removeEventListener(TextTrackListEventTypes.ADDTRACK, onAddTrack)
        player.textTracks.removeEventListener(TextTrackListEventTypes.REMOVETRACK, onRemoveTrack)
    }
}

private data class YospaceReport(
    var ymid: String? = null,
    var ytyp: String? = null,
    var yseq: String? = null,
    var ydur: String? = null,
) {
    fun update(key: String, value: String?) {
        when (key.uppercase()) {
            "YMID" -> ymid = value
            "YTYP" -> ytyp = value
            "YSEQ" -> yseq = value
            "YDUR" -> ydur = value
        }
    }

    fun finish(playhead: Long): TimedMetadata? {
        val (ymid, ytyp, yseq, ydur) = this
        // Only create complete reports
        return if (ymid != null && ydur != null && yseq != null && ytyp != null) {
            TimedMetadata.createFromMetadata(ymid, yseq, ytyp, ydur, playhead)
        } else {
            null
        }
    }
}

private fun isYospaceId3Cue(cue: TextTrackCue): Boolean {
    val id3 = cue.content?.optJSONObject("content") ?: return false
    return when (id3.optString("id")) {
        "YMID", "YTYP", "YSEQ", "YDUR" -> true
        else -> false
    }
}

private fun isYospaceEmsgCue(cue: TextTrackCue): Boolean {
    // FIXME Check if cue.schemeIdUri == YOSPACE_EMSG_SCHEME_ID_URI
    return cue.content?.optJSONArray("content") != null
}

private fun jsonArrayToByteArray(jsonArray: JSONArray): ByteArray {
    val result = ByteArray(jsonArray.length())
    for (i in 0..<jsonArray.length()) {
        result[i] = jsonArray.getInt(i).toByte()
    }
    return result
}