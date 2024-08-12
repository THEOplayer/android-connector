package com.theoplayer.android.connector.uplynk.internal

import android.os.Handler
import android.os.Looper
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.connector.uplynk.common.UplynkEventDispatcher
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import com.theoplayer.android.connector.uplynk.internal.events.UplynkAssetInfoErrorResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.events.UplynkAssetInfoResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.events.UplynkPreplayErrorResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.events.UplynkPreplayResponseEventImpl
import com.theoplayer.android.connector.uplynk.internal.network.AssetInfoInternalResponse
import com.theoplayer.android.connector.uplynk.internal.network.PreplayInternalResponse
import com.theoplayer.android.connector.uplynk.network.UplynkApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.Date

internal class UplynkAdIntegration(
    val theoplayerView: THEOplayerView,
    val controller: ServerSideAdIntegrationController,
    val eventDispatcher: UplynkEventDispatcher,
    val uplynkDescriptionConverter: UplynkSsaiDescriptionConverter
) : ServerSideAdIntegrationHandler {

    private val player: Player
        get() = theoplayerView.player

    private val uplynkApi = UplynkApi()
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    override suspend fun setSource(source: SourceDescription): SourceDescription {

        val uplynkSource = source.sources.singleOrNull { it.ssai is UplynkSsaiDescription }
        val ssaiDescription = uplynkSource?.ssai as? UplynkSsaiDescription ?: return source

        val response = uplynkDescriptionConverter
            .buildPreplayUrl(ssaiDescription)
            .let { uplynkApi.preplay(it) }
            .also { dispatchPreplayEvents(it) }


        val newSource = source.replaceSources(source.sources.toMutableList().apply {
            remove(uplynkSource)
            add(0, uplynkSource.replaceSrc(response.internalResponse.playURL))
        })
        if (ssaiDescription.assetInfo) {
            uplynkDescriptionConverter
                .buildAssetInfoUrls(ssaiDescription, response.internalResponse.sid)
                .map { uplynkApi.assetInfo(it) }
                .forEach { dispatchAssetInfoEvents(it) }
        }

        return newSource
    }

    private fun dispatchPreplayEvents(response: PreplayInternalResponse) {
        if (response.externalResponse != null) {
            eventDispatcher.dispatchEvent(
                UplynkPreplayResponseEventImpl(
                    Date(),
                    response.externalResponse!!
                )
            )
        } else {
            eventDispatcher.dispatchEvent(
                UplynkPreplayErrorResponseEventImpl(
                    Date(),
                    response.body,
                    response.error
                )
            )
        }
    }

    private fun dispatchAssetInfoEvents(assetInfo: AssetInfoInternalResponse) =
        CoroutineScope(Dispatchers.IO).async {
            mainThreadHandler.post {
                if (assetInfo.externalResponse != null) {
                    eventDispatcher.dispatchEvent(
                        UplynkAssetInfoResponseEventImpl(
                            Date(),
                            assetInfo.externalResponse!!
                        )
                    )
                } else {
                    eventDispatcher.dispatchEvent(
                        UplynkAssetInfoErrorResponseEventImpl(
                            Date(),
                            assetInfo.body,
                            assetInfo.error
                        )
                    )
                }

            }
        }

}
