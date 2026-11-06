package com.theoplayer.android.connector.lens

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.EventType
import com.theoplayer.android.api.event.player.PlayerEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.pip.PiPType
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.metadata.MetadataDescription
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

/** The metadata key of the content ID the demo reports to Lens for a source. */
const val LENS_CONTENT_ID = "lensContentId"

/**
 * The basic playback scenario the Lens connector demos play on every platform, so their reports can be compared in the
 * backend: same stream, same content ID, same steps. Durations count from when the player confirms the step: playing,
 * a seek completed, picture-in-picture entered.
 */
class LensScenario(private val view: THEOplayerView) {
    private val player = view.player

    /** The steps, in order, as shown while the scenario runs. */
    val steps = listOf(
        "Load Big Buck Bunny" to suspend { loadSource() },
        "Play 10 s" to suspend { play(); delay(10_000) },
        "Pause 3 s" to suspend { after(PlayerEventTypes.PAUSE) { player.pause() }; delay(3_000) },
        "Play 5 s" to suspend { play(); delay(5_000) },
        "Seek to 120 s, play 5 s" to suspend { seek(120.0); delay(5_000) },
        "Mute 3 s, unmute 2 s" to suspend { player.isMuted = true; delay(3_000); player.isMuted = false; delay(2_000) },
        "Rate x2 5 s, x1 2 s" to suspend { player.playbackRate = 2.0; delay(5_000); player.playbackRate = 1.0; delay(2_000) },
        "Picture-in-picture 5 s, inline 2 s" to suspend { pictureInPicture(true); delay(5_000); pictureInPicture(false); delay(2_000) },
        "Seek to 5 s before the end, play to the end" to suspend {
            seek(player.duration - 5)
            after(PlayerEventTypes.ENDED, timeoutMs = 90_000) {}
            delay(2_000)
        },
        "Unload" to suspend { player.source = null },
    )

    /** Runs every step; [onStep] gets the index of the step that starts. */
    suspend fun run(onStep: (Int) -> Unit) {
        steps.forEachIndexed { index, (_, step) ->
            onStep(index)
            step()
        }
    }

    private fun loadSource() {
        player.source = SourceDescription(
            listOf(TypedSource("https://cdn.theoplayer.com/video/big_buck_bunny/big_buck_bunny.m3u8")),
            metadata = MetadataDescription(mutableMapOf<String, Any?>(LENS_CONTENT_ID to "lens-connector-demo-scenario")),
        )
    }

    private suspend fun play() = after(PlayerEventTypes.PLAYING) { player.play() }

    private suspend fun seek(time: Double) = after(PlayerEventTypes.SEEKED) { player.currentTime = time }

    /** Android's system picture-in-picture, with the player in THEOplayer's own PiP activity; there is no in-app one. */
    private suspend fun pictureInPicture(enter: Boolean) {
        val pip = checkNotNull(view.piPManager) { "Picture-in-picture is not configured on the player" }
        after(PlayerEventTypes.PRESENTATIONMODECHANGE) { if (enter) pip.enterPiP(PiPType.ACTIVITY) else pip.exitPiP() }
    }

    /** Runs [action] and waits for the player's next [type] event, listening before the action so none is missed. */
    private suspend fun <E : PlayerEvent<*>> after(type: EventType<E>, timeoutMs: Long = 60_000, action: () -> Unit) {
        withTimeout(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                lateinit var listener: EventListener<E>
                listener = EventListener {
                    player.removeEventListener(type, listener)
                    continuation.resume(Unit)
                }
                player.addEventListener(type, listener)
                continuation.invokeOnCancellation { player.removeEventListener(type, listener) }
                action()
            }
        }
    }
}
