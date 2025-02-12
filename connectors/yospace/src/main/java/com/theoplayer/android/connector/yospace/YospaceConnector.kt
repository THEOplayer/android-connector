package com.theoplayer.android.connector.yospace

import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.Ad
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionRegistry
import com.theoplayer.android.connector.yospace.internal.YospaceAdIntegration
import com.yospace.admanagement.AdBreak
import com.yospace.admanagement.Advert
import com.yospace.admanagement.AnalyticEventObserver
import com.yospace.admanagement.Session
import com.yospace.admanagement.TrackingErrors
import java.util.concurrent.CopyOnWriteArrayList

internal const val TAG = "YospaceConnector"
internal const val USER_AGENT = "THEOplayerYospaceConnector/${BuildConfig.LIBRARY_VERSION}"

/**
 * A connector for the Yospace Ad Management SDK.
 *
 * @param theoplayerView
 *   The THEOplayer view, which will be connected to the created connector.
 * @param uiHandler
 *   A handler for updating the UI. By default, this creates a [YospaceDefaultUiHandler].
 *
 * @sample com.theoplayer.android.connector.yospace.samples.createYospaceConnector
 *
 * @see [Yospace Ad Management SDK v3 for Android Developers](https://developer.yospace.com/sdk-documentation/android/userguide/latest/en/index-en.html)
 *      (requires login)
 */
class YospaceConnector @JvmOverloads constructor(
    private val theoplayerView: THEOplayerView,
    private val uiHandler: YospaceUiHandler = YospaceDefaultUiHandler(theoplayerView)
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

    /**
     * Registers an [AnalyticEventObserver] on the Yospace [Session].
     */
    fun registerAnalyticEventObserver(observer: AnalyticEventObserver) {
        analyticEventObservers.add(observer)
    }

    /**
     * Removes a previously registered [AnalyticEventObserver].
     */
    fun unregisterAnalyticEventObserver(observer: AnalyticEventObserver) {
        analyticEventObservers.remove(observer)
    }

    /**
     * Registers a [YospaceListener] to receive events from this connector.
     */
    fun addListener(listener: YospaceListener) {
        listeners.add(listener)
    }

    /**
     * Removes a previously registered [YospaceListener].
     */
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
        /**
         * The integration identifier for the Yospace connector.
         *
         * Ads created by this connector have this value as their [custom integration][Ad.getCustomIntegration].
         */
        const val INTEGRATION_ID = "yospace"

        init {
            CustomSsaiDescriptionRegistry.register(INTEGRATION_ID, YospaceSsaiDescriptionSerializer)
        }
    }
}
