package com.theoplayer.android.connector

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ima.GoogleImaIntegrationFactory
import com.theoplayer.android.connector.analytics.comscore.ComscoreConfiguration
import com.theoplayer.android.connector.analytics.comscore.ComscoreConnector
import com.theoplayer.android.connector.analytics.comscore.ComscoreMediaType
import com.theoplayer.android.connector.analytics.comscore.ComscoreMetaData
import com.theoplayer.android.connector.analytics.conviva.ConvivaConfiguration
import com.theoplayer.android.connector.analytics.conviva.ConvivaConnector
import com.theoplayer.android.connector.analytics.nielsen.NielsenConnector
import com.theoplayer.android.connector.yospace.YospaceConnector

class MainActivity : AppCompatActivity() {

    private lateinit var theoplayerView: THEOplayerView
    private lateinit var convivaConnector: ConvivaConnector
    private lateinit var nielsenConnector: NielsenConnector
    private lateinit var comscoreConnector: ComscoreConnector
    private lateinit var yospaceConnector: YospaceConnector
    private var selectedSource: Source = sources.first()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupTHEOplayer()
        setupGoogleImaIntegration()
        setupConviva()
        setupComscore()
        setupNielsen()
        setupYospace()
    }

    private fun setupComscore() {
        val comscoreConfiguration = ComscoreConfiguration(
            publisherId = "<your publisher ID here>", // Can be a test or production key.
            applicationName = "THEOplayer Demo",
            userConsent = "1",
            childDirected = false,
            secureTransmission = true,
            usagePropertiesAutoUpdateMode = 1,
            debug = true
        )
        val metadata = ComscoreMetaData(
            mediaType = ComscoreMediaType.LONG_FORM_ON_DEMAND,
            uniqueId = "testuniqueId",
            length = 634,
            stationTitle = "THEOTV",
            programTitle = "Big Buck Bunny",
            episodeTitle = "Intro",
            genreName = "Animation",
            classifyAsAudioStream = false,
            c3 = "c3value",
            c4 = "c4value",
            c6 = "c6value",
            stationCode = null,
            networkAffiliate = null,
            publisherName = null,
            programId = null,
            episodeId = null,
            episodeSeasonNumber = null,
            episodeNumber = null,
            genreId = null,
            carryTvAdvertisementLoad = null,
            classifyAsCompleteEpisode = null,
            dateOfProduction = null,
            timeOfProduction = null,
            dateOfTvAiring = null,
            timeOfTvAiring = null,
            dateOfDigitalAiring = null,
            timeOfDigitalAiring = null,
            feedType = null,
            deliveryMode = null,
            deliverySubscriptionType = null,
            deliveryComposition = null,
            deliveryAdvertisementCapability = null,
            mediaFormat = null,
            distributionModel = null,
            playlistTitle = null,
            totalSegments = null,
            clipUrl = null,
            videoDimension = null,
            customLabels = emptyMap(),
        )
        comscoreConnector =
            ComscoreConnector(applicationContext, theoplayerView.player, comscoreConfiguration, metadata)
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

        val metadata = hashMapOf(
            "Conviva.applicationName" to "THEOplayer",
            "Conviva.viewerId" to "viewerId"
        )

        val config = ConvivaConfiguration(
            customerKey,
            true,
            gatewayUrl,
        )
        convivaConnector = ConvivaConnector(applicationContext, theoplayerView.player, metadata, config)
    }

    private fun setupNielsen() {
        val appId = "your_nielsen_app_id"
        nielsenConnector = NielsenConnector(applicationContext, theoplayerView.player, appId, true)
    }

    private fun setupYospace() {
        yospaceConnector = YospaceConnector(theoplayerView.player)
    }

    fun selectSource(view: View) {
        val sourceNames = sources.map { it.name }.toTypedArray()
        val selectedIndex = sources.indexOf(selectedSource)
        AlertDialog.Builder(this)
            .setTitle("Select source")
            .setSingleChoiceItems(sourceNames, selectedIndex) { dialog, which ->
                setSource(sources[which])
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun setSource(source: Source) {
        selectedSource = source
        theoplayerView.player.source = source.sourceDescription
        nielsenConnector.updateMetadata(source.nielsenMetadata)
    }

    fun playPause(view: View) {
        if (theoplayerView.player.isPaused) {
            theoplayerView.player.play()
        } else {
            theoplayerView.player.pause()
        }
    }

    fun seekBackward(view: View) {
        theoplayerView.player.currentTime -= 10
    }

    fun seekForward(view: View) {
        theoplayerView.player.currentTime += 10
    }

    override fun onPause() {
        super.onPause()
        theoplayerView.onPause()
    }

    override fun onResume() {
        super.onResume()
        theoplayerView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        theoplayerView.onDestroy()
    }
}