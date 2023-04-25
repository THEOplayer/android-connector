package com.theoplayer.android.connector

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.conviva.api.SystemSettings
import com.conviva.sdk.ConvivaSdkConstants
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ima.GoogleImaIntegrationFactory
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription
import com.theoplayer.android.connector.analytics.conviva.ConvivaConnector
import com.theoplayer.android.connector.analytics.nielsen.NielsenConnector

class MainActivity : AppCompatActivity() {

    private lateinit var theoplayerView: THEOplayerView
    private var convivaConnector: ConvivaConnector? = null
    private var nielsenConnector: NielsenConnector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupTHEOplayer()
        setupGoogleImaIntegration()
        setupConviva()
        setupNielsen()
    }

    private fun setupTHEOplayer() {
        val theoplayerConfig = THEOplayerConfig.Builder()
            .build()
        theoplayerView = THEOplayerView(this, theoplayerConfig)
        theoplayerView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        val tpvContainer = findViewById<FrameLayout>(R.id.tpv_container)
        tpvContainer.addView(theoplayerView)
    }

    private fun setupGoogleImaIntegration() {
        val googleImaIntegration = GoogleImaIntegrationFactory.createGoogleImaIntegration(theoplayerView)
        theoplayerView.player.addIntegration(googleImaIntegration)
    }

    private fun setupConviva() {
        val customerKey = "your_conviva_customer_key"
        val gatewayUrl = "your_conviva_debug_gateway_url"

        val settings = HashMap<String, Any>()
        settings[ConvivaSdkConstants.GATEWAY_URL] = gatewayUrl
        settings[ConvivaSdkConstants.LOG_LEVEL] = SystemSettings.LogLevel.DEBUG

        convivaConnector = ConvivaConnector(applicationContext, theoplayerView.player, customerKey, settings)
        convivaConnector?.setViewerId("viewer ID")
    }

    private fun setupNielsen() {
        val appId = "your_nielsen_app_id"
        nielsenConnector = NielsenConnector(applicationContext, theoplayerView.player, appId, true)
    }

    fun setSource(view: View) {
        theoplayerView.player.source = SourceDescription.Builder(
            TypedSource.Builder("https://cdn.theoplayer.com/video/big_buck_bunny/big_buck_bunny.m3u8")
                .build()
        )
        .ads(
            GoogleImaAdDescription.Builder("https://cdn.theoplayer.com/demos/ads/vast/dfp-linear-inline-no-skip.xml")
                .timeOffset("5")
                .build()
        )
        .build()
        convivaConnector?.setAssetName("BigBuckBunny with Google IMA ads")
        nielsenConnector?.updateMetadata(hashMapOf(
            "assetid" to "C112233",
            "program" to "BigBuckBunny with Google IMA ads"
        ))
    }

    fun playPause(view: View) {
        if (theoplayerView.player.isPaused) {
            theoplayerView.player.play()
        } else {
            theoplayerView.player.pause()
        }
    }

    fun seekBackward(view: View) {
        theoplayerView.player.currentTime = theoplayerView.player.currentTime - 10
    }

    fun seekForward(view: View) {
        theoplayerView.player.currentTime = theoplayerView.player.currentTime + 10
    }

}