package com.theoplayer.android.connector.yospace

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionRegistry
import com.theoplayer.android.connector.yospace.internal.YospaceAdIntegration
import com.yospace.admanagement.AdBreak
import com.yospace.admanagement.Advert
import com.yospace.admanagement.AnalyticEventObserver
import com.yospace.admanagement.Session
import com.yospace.admanagement.TrackingErrors
import java.util.concurrent.CopyOnWriteArrayList

const val INTEGRATION_ID = "yospace"
internal const val TAG = "YospaceConnector"
internal const val USER_AGENT = "THEOplayerYospaceConnector/${BuildConfig.LIBRARY_VERSION}"

class YospaceConnector @JvmOverloads constructor(
    private val theoplayerView: THEOplayerView,
    private val uiHandler: YospaceUiHandler = DefaultYospaceUiHandler(theoplayerView)
) {
    private val analyticEventObservers = CopyOnWriteArrayList<AnalyticEventObserver>()
    private val listeners = CopyOnWriteArrayList<YospaceListener>()

    private lateinit var integration: YospaceAdIntegration

    init {
        theoplayerView.player.ads.registerServerSideIntegration(INTEGRATION_ID, this::setupIntegration)
    }

    private fun setupIntegration(controller: ServerSideAdIntegrationController): YospaceAdIntegration {
        val integration = YospaceAdIntegration(
            theoplayerView,
            uiHandler,
            controller,
            ForwardingAnalyticEventObserver(),
            ForwardingListener()
        )
        this.integration = integration
        return integration
    }

    fun registerAnalyticEventObserver(observer: AnalyticEventObserver) {
        analyticEventObservers.add(observer)
    }

    fun unregisterAnalyticEventObserver(observer: AnalyticEventObserver) {
        analyticEventObservers.remove(observer)
    }

    fun addListener(listener: YospaceListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: YospaceListener) {
        listeners.remove(listener)
    }

    private inner class ForwardingAnalyticEventObserver : AnalyticEventObserver {
        override fun onAdvertBreakStart(adBreak: AdBreak?, session: Session) {
            analyticEventObservers.forEach { it.onAdvertBreakStart(adBreak, session) }
        }

        override fun onAdvertBreakEnd(session: Session) {
            analyticEventObservers.forEach { it.onAdvertBreakEnd(session) }
        }

        override fun onAdvertStart(advert: Advert, session: Session) {
            analyticEventObservers.forEach { it.onAdvertStart(advert, session) }
        }

        override fun onAdvertEnd(session: Session) {
            analyticEventObservers.forEach { it.onAdvertEnd(session) }
        }

        override fun onAnalyticUpdate(session: Session) {
            analyticEventObservers.forEach { it.onAnalyticUpdate(session) }
        }

        override fun onEarlyReturn(adBreak: AdBreak, session: Session) {
            analyticEventObservers.forEach { it.onEarlyReturn(adBreak, session) }
        }

        override fun onSessionError(error: AnalyticEventObserver.SessionError, session: Session) {
            analyticEventObservers.forEach { it.onSessionError(error, session) }
        }

        override fun onTrackingEvent(type: String, session: Session) {
            analyticEventObservers.forEach { it.onTrackingEvent(type, session) }
        }

        override fun onTrackingError(error: TrackingErrors.Error, session: Session) {
            analyticEventObservers.forEach { it.onTrackingError(error, session) }
        }
    }

    private inner class ForwardingListener : YospaceListener {
        override fun onSessionAvailable() {
            listeners.forEach { it.onSessionAvailable() }
        }
    }

    companion object {
        init {
            CustomSsaiDescriptionRegistry.register(INTEGRATION_ID, YospaceSsaiDescriptionSerializer())
        }
    }
}
