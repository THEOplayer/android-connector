package com.theoplayer.android.connector.analytics.lens.internal

import com.theoplayer.android.api.player.Player

/**
 * THEOplayer's `player.cmcd.linkedSessionId`, which links the player's own CMCD reporting to the Lens session (`lsid`).
 * Reached by reflection: THEOplayer versions before `Player.getCmcd()` don't have it, and then this does nothing.
 */
internal class CmcdSessionLink(player: Player) {
    private val setLinkedSessionId: ((String?) -> Unit)? =
        try {
            val getCmcd = player.javaClass.getMethod("getCmcd")
            val setter = getCmcd.returnType.getMethod("setLinkedSessionId", String::class.java)
            val cmcd = getCmcd.invoke(player)
            val link: (String?) -> Unit = { sessionId -> setter.invoke(cmcd, sessionId) }
            link
        } catch (_: NoSuchMethodException) {
            null
        }

    fun set(sessionId: String?) {
        setLinkedSessionId?.invoke(sessionId)
    }
}
