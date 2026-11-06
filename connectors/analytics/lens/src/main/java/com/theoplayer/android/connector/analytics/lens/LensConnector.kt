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

/**
 * The connector between a THEOplayer and Dolby OptiView Lens.
 *
 * The connector joins Lens with a player adapter for the given player, so Lens reports playback on its own: sessions,
 * playback states, errors and the player's CMCD keys.
 *
 * @param theoplayerView the view of the player to report.
 * @param joinOptions the options to join Lens with; its `player` is replaced by this connector's [adapter].
 * @param contentIdProvider returns the content ID to report for a source. Without it, or when it returns null, Lens
 * derives one from the source URL. Required to report sources without a URL, such as Google DAI streams.
 */
class LensConnector @JvmOverloads constructor(
    theoplayerView: THEOplayerView,
    joinOptions: LensJoinOptions,
    contentIdProvider: ContentIdProvider? = null,
) {
    /**
     * The Lens player adapter for the player. Another SDK reporting to Lens joins with this same adapter to share the
     * player's session.
     */
    val adapter: LensPlayerAdapter = THEOplayerLensAdapter(theoplayerView, contentIdProvider)

    /** The Lens handle the connector joined with. Use it to emit your own events, set consent, or flush. */
    val handle: LensHandle = Lens.join(joinOptions.copy(player = adapter))

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
