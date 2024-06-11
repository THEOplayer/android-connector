package com.theoplayer.android.connector

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.LinearAd
import com.theoplayer.android.api.ads.ima.GoogleImaIntegrationFactory
import com.theoplayer.android.api.event.ads.AdBreakEvent
import com.theoplayer.android.api.event.ads.AdsEventTypes
import com.theoplayer.android.api.event.ads.SingleAdEvent
import com.theoplayer.android.connector.analytics.comscore.ComscoreConfiguration
import com.theoplayer.android.connector.analytics.comscore.ComscoreConnector
import com.theoplayer.android.connector.analytics.comscore.ComscoreMediaType
import com.theoplayer.android.connector.analytics.comscore.ComscoreMetaData
import com.theoplayer.android.connector.analytics.conviva.ConvivaConfiguration
import com.theoplayer.android.connector.analytics.conviva.ConvivaConnector
import com.theoplayer.android.connector.analytics.nielsen.NielsenConnector
import com.theoplayer.android.connector.yospace.YospaceConnector

const val TAG = "MainActivity"

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
        setupListeners()
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
        yospaceConnector = YospaceConnector(theoplayerView)
    }

    private fun setupListeners() {
        val ads = theoplayerView.player.ads
        ads.addEventListener(AdsEventTypes.ADD_AD, ::onAdEvent)
        ads.addEventListener(AdsEventTypes.AD_BEGIN, ::onAdEvent)
        ads.addEventListener(AdsEventTypes.AD_END, ::onAdEvent)
        ads.addEventListener(AdsEventTypes.AD_SKIP, ::onAdEvent)
        ads.addEventListener(AdsEventTypes.AD_FIRST_QUARTILE, ::onAdEvent)
        ads.addEventListener(AdsEventTypes.AD_MIDPOINT, ::onAdEvent)
        ads.addEventListener(AdsEventTypes.AD_THIRD_QUARTILE, ::onAdEvent)
        ads.addEventListener(AdsEventTypes.ADD_AD_BREAK, ::onAdBreakEvent)
        ads.addEventListener(AdsEventTypes.AD_BREAK_BEGIN, ::onAdBreakEvent)
        ads.addEventListener(AdsEventTypes.AD_BREAK_END, ::onAdBreakEvent)
        ads.addEventListener(AdsEventTypes.AD_BREAK_CHANGE, ::onAdBreakEvent)
        ads.addEventListener(AdsEventTypes.REMOVE_AD_BREAK, ::onAdBreakEvent)
    }

    fun onAdBreakEvent(event: AdBreakEvent<*>) {
        val adBreak = event.adBreak
        Log.d(
            TAG,
            "${event.type} - " +
                    "timeOffset=${adBreak.timeOffset}, ads=${adBreak.ads.size}," +
                    " maxDuration=${adBreak.maxDuration}," +
                    " currentTime=${theoplayerView.player.currentTime}"
        )
    }

    fun onAdEvent(event: SingleAdEvent<*>) {
        val ad = event.ad ?: return
        Log.d(
            TAG,
            "${event.type} - " +
                    "id=${ad.id}, type=${ad.type}, adBreak.timeOffset=${ad.adBreak?.timeOffset}," +
                    (if (ad is LinearAd) " duration=${ad.duration}," else "") +
                    " currentTime=${theoplayerView.player.currentTime}"
        )
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