package com.theoplayer.android.connector.analytics.lens

import com.dolby.optiview.lens.Lens
import com.dolby.optiview.lens.LensHandle
import com.dolby.optiview.lens.LensJoinOptions
import com.dolby.optiview.lens.LensPlayerAdapter
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.connector.analytics.lens.internal.THEOplayerLensAdapter

/**
 * Reports a THEOplayer to OptiView Lens.
 *
 * The connector joins Lens with [joinOptions] on behalf of the player: Lens then starts and ends the player's sessions
 * on its own and reports its playback in the `lens` namespace.
 *
 * Do not configure THEOplayer's own CMCD reporting for a player that Lens observes.
 *
 * @param theoplayerView the view of the player to report.
 * @param joinOptions how to join Lens; its `player` is replaced by this connector's [playerAdapter].
 */
class LensConnector(
    theoplayerView: THEOplayerView,
    joinOptions: LensJoinOptions,
) {
    /**
     * The player as Lens sees it. Hand this same adapter to every other SDK that joins Lens for this player, so they
     * report into the player's session.
     */
    val playerAdapter: LensPlayerAdapter = THEOplayerLensAdapter(theoplayerView)

    /** The Lens handle this connector joined with, e.g. to emit the application's own events into the player's session. */
    val handle: LensHandle = Lens.join(joinOptions.copy(player = playerAdapter))

    /**
     * Leaves Lens, ending the player's session unless another handle joined with [playerAdapter] remains.
     * The connector can no longer be used afterwards.
     */
    fun destroy() {
        handle.dispose()
    }
}
