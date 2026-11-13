package com.theoplayer.android.connector.analytics.lens

import com.dolby.optiview.lens.Lens
import com.dolby.optiview.lens.LensHandle
import com.dolby.optiview.lens.LensJoinOptions
import com.dolby.optiview.lens.LensPlayerAdapter
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.DestroyEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.connector.analytics.lens.internal.THEOplayerLensAdapter

private const val NAMESPACE = "theoplayer"

/**
 * The connector between a THEOplayer and Dolby OptiView Lens.
 *
 * The connector joins Lens with a player adapter for the given player, so Lens reports playback on its own: sessions,
 * playback states, errors and the player's CMCD keys.
 *
 * @param theoplayerView the view of the player to report.
 * @param configuration how to join Lens.
 */
class LensConnector @JvmOverloads constructor(
    theoplayerView: THEOplayerView,
    configuration: LensConfiguration = LensConfiguration(),
) {
    /**
     * The Lens player adapter for the player. Its `target` is THEOplayer's `Player`: to report events of your own, join
     * Lens in your own namespace with that player, and your events share the player's session; another SDK that reports
     * this player to Lens joins the same way.
     */
    val adapter: LensPlayerAdapter = THEOplayerLensAdapter(theoplayerView, configuration.contentIdProvider)

    /** The Lens handle the connector joined with, in the `theoplayer` namespace. Use it to set consent or flush. */
    val handle: LensHandle =
        LensJoinOptions(NAMESPACE, player = adapter).let { defaults ->
            Lens.join(
                defaults.copy(
                    destinations = configuration.destinations,
                    consent = configuration.consent,
                    strict = configuration.strict,
                    heartbeatMs = configuration.heartbeatMs ?: defaults.heartbeatMs,
                    batch = configuration.batch,
                ),
            )
        }

    private val player: Player = theoplayerView.player
    private val onDestroy = EventListener<DestroyEvent> { destroy() }

    init {
        player.addEventListener(PlayerEventTypes.DESTROY, onDestroy)
    }

    /**
     * Ends the session and stops reporting the player. The connector is destroyed with its player.
     */
    fun destroy() {
        player.removeEventListener(PlayerEventTypes.DESTROY, onDestroy)
        handle.dispose()
    }
}
