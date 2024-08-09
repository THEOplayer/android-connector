package com.theoplayer.android.connector.uplynk.internal

import android.os.Handler
import android.os.Looper
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.connector.uplynk.UplynkEventDispatcherImpl
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import com.theoplayer.android.connector.uplynk.internal.events.UplynkAssetInfoResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.events.UplynkPreplayResponseEventImpl
import com.theoplayer.android.connector.uplynk.network.UplynkApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.Date

internal class UplynkAdIntegration(
    val theoplayerView: THEOplayerView,
    val controller: ServerSideAdIntegrationController,
    val eventDispatcher: UplynkEventDispatcherImpl,
    val uplynkDescriptionConverter: UplynkSsaiDescriptionConverter
) : ServerSideAdIntegrationHandler {

    private val player: Player
        get() = theoplayerView.player

    private val uplynkApi = UplynkApi()
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override suspend fun setSource(source: SourceDescription): SourceDescription {
        val uplynkSource = source.sources.find { it.ssai is UplynkSsaiDescription } ?: return source
        val ssaiDescription = uplynkSource.ssai as? UplynkSsaiDescription ?: return source
        val response = uplynkApi.preplay(uplynkDescriptionConverter.buildPreplayUrl(ssaiDescription))
        eventDispatcher.dispatchEvent(UplynkPreplayResponseEventImpl(Date(), response))

        val newSource = source.replaceSources(source.sources.toMutableList().apply {
            remove(uplynkSource)
            add(0, uplynkSource.replaceSrc(response.playURL))
        })
        if (ssaiDescription.assetInfo) {
            uplynkDescriptionConverter
                .buildAssetInfoUrls(ssaiDescription, response.sid)
                .map { url ->
                    ioScope.async {
                        val assetInfo = uplynkApi.assetInfo(url)
                        mainThreadHandler.post {
                            eventDispatcher.dispatchEvent(UplynkAssetInfoResponseEventImpl(Date(), assetInfo))
                        }
                    }
                }
        }

        return newSource
    }
}
