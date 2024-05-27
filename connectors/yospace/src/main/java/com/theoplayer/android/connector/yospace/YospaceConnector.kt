package com.theoplayer.android.connector.yospace

import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.connector.yospace.internal.YospaceAdIntegration
import java.util.concurrent.CopyOnWriteArrayList

const val INTEGRATION_ID = "yospace"
const val TAG = "YospaceConnector"

class YospaceConnector(
    player: Player
) {
    private val listeners = CopyOnWriteArrayList<YospaceListener>()

    private lateinit var integration: YospaceAdIntegration

    init {
        player.ads.registerServerSideIntegration(INTEGRATION_ID, this::setupIntegration)
    }

    private fun setupIntegration(controller: ServerSideAdIntegrationController): YospaceAdIntegration {
        val integration = YospaceAdIntegration(
            controller,
            ForwardingListener()
        )
        this.integration = integration
        return integration
    }

    fun addListener(listener: YospaceListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: YospaceListener) {
        listeners.remove(listener)
    }

    private inner class ForwardingListener : YospaceListener {
        override fun onSessionAvailable() {
            listeners.forEach { it.onSessionAvailable() }
        }
    }
}
