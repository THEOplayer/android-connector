package com.theoplayer.android.connector.analytics.gemius

import android.content.Context
import com.theoplayer.android.api.THEOplayerView
import com.gemius.sdk.stream.Player
import com.gemius.sdk.stream.PlayerData
import com.gemius.sdk.stream.ProgramData

val PLAYER_ID = "THEOplayer"

class GemiusAdapter(
    context: Context,
    configuration: GemiusConfiguration,
    playerView: THEOplayerView,
    adProcessor: AdProcessor?
) {
    private val gemiusPlayer: Player?
    private var programId: String? = null
    private var programData: ProgramData? = null

    init {
        val playerData = PlayerData()
        playerData.resolution = "${playerView.width}x${playerView.height}"
        playerData.volume = if (playerView.player.isMuted) -1 else (playerView.player.volume * 100).toInt()
        gemiusPlayer = Player(PLAYER_ID, configuration.hitCollectorHost, configuration.gemiusId, playerData)
        gemiusPlayer.setContext(context)

    }

    fun update(programId: String, programData: ProgramData) {
        this.programId = programId
        this.programData = programData
    }

}