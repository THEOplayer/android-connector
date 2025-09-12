package com.theoplayer.android.connector

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import com.theoplayer.android.connector.uplynk.SkippedAdStrategy
import com.theoplayer.android.connector.uplynk.UplynkConfiguration
import com.theoplayer.android.connector.uplynk.UplynkConnector
import com.theoplayer.android.connector.uplynk.UplynkListener
import com.theoplayer.android.connector.uplynk.network.AssetInfoResponse
import com.theoplayer.android.connector.uplynk.network.PingResponse
import com.theoplayer.android.connector.uplynk.network.PreplayLiveResponse
import com.theoplayer.android.connector.uplynk.network.PreplayVodResponse
import com.theoplayer.android.connector.yospace.YospaceConnector
import com.theoplayer.android.ui.DefaultUI
import com.theoplayer.android.ui.rememberPlayer
import com.theoplayer.android.ui.theme.THEOplayerTheme

const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var theoplayerView: THEOplayerView
    private lateinit var convivaConnector: ConvivaConnector
    private lateinit var nielsenConnector: NielsenConnector
    private lateinit var comscoreConnector: ComscoreConnector
    private lateinit var yospaceConnector: YospaceConnector
    private lateinit var uplynkConnector: UplynkConnector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupTHEOplayer()
        setupGoogleImaIntegration()
        setupConviva()
        setupComscore()
        setupNielsen()
        setupYospace()
        setupUplynk()
        setupAdListeners()

        setContent {
            THEOplayerTheme(useDarkTheme = true) {
                MainContent(theoplayerView, nielsenConnector)
            }
        }
    }

    private fun setupTHEOplayer() {
        val theoplayerConfig = THEOplayerConfig.Builder()
            .build()
        theoplayerView = THEOplayerView(this, theoplayerConfig)
        theoplayerView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    private fun setupGoogleImaIntegration() {
        val googleImaIntegration =
            GoogleImaIntegrationFactory.createGoogleImaIntegration(theoplayerView)
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
        convivaConnector =
            ConvivaConnector(applicationContext, theoplayerView.player, metadata, config)
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
        comscoreConnector = ComscoreConnector(
            applicationContext,
            theoplayerView.player,
            comscoreConfiguration,
            metadata
        )
    }

    private fun setupNielsen() {
        val appId = "your_nielsen_app_id"
        nielsenConnector = NielsenConnector(applicationContext, theoplayerView.player, appId, true)
    }

    private fun setupYospace() {
        yospaceConnector = YospaceConnector(theoplayerView)
    }

    private fun setupUplynk() {
        uplynkConnector = UplynkConnector(
            theoplayerView,
            UplynkConfiguration(defaultSkipOffset = 5, SkippedAdStrategy.PLAY_LAST)
        )
        uplynkConnector.addListener(object : UplynkListener {
            override fun onPreplayVodResponse(response: PreplayVodResponse) {
                Log.d("UplynkConnectorEvents", "PREPLAY_VOD_RESPONSE $response")
            }

            override fun onPreplayLiveResponse(response: PreplayLiveResponse) {
                Log.d("UplynkConnectorEvents", "PREPLAY_LIVE_RESPONSE $response")
            }

            override fun onAssetInfoResponse(response: AssetInfoResponse) {
                Log.d("UplynkConnectorEvents", "ASSET_INFO_RESPONSE $response")
            }

            override fun onPreplayFailure(exception: Exception) {
                Log.d("UplynkConnectorEvents", "PREPLAY_RESPONSE_FAILURE $exception")
            }

            override fun onAssetInfoFailure(exception: Exception) {
                Log.d("UplynkConnectorEvents", "ASSET_INFO_RESPONSE Failure $exception")
            }

            override fun onPingResponse(pingResponse: PingResponse) {
                Log.d("UplynkConnectorEvents", "PING_RESPONSE $pingResponse")
            }
        })
    }

    private fun setupAdListeners() {
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

    private fun onAdBreakEvent(event: AdBreakEvent<*>) {
        val adBreak = event.adBreak
        Log.d(
            TAG, "${event.type} - " +
                    "timeOffset=${adBreak.timeOffset}, " +
                    "ads=${adBreak.ads.size}, " +
                    "maxDuration=${adBreak.maxDuration}, " +
                    "currentTime=${theoplayerView.player.currentTime}"
        )
    }

    private fun onAdEvent(event: SingleAdEvent<*>) {
        val ad = event.ad ?: return
        Log.d(
            TAG, "${event.type} - " +
                    "id=${ad.id}, " +
                    "type=${ad.type}, " +
                    "adBreak.timeOffset=${ad.adBreak?.timeOffset}, " +
                    (if (ad is LinearAd) "duration=${ad.duration}, " else "") +
                    "currentTime=${theoplayerView.player.currentTime}"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    theoplayerView: THEOplayerView,
    nielsenConnector: NielsenConnector,
) {
    val player = rememberPlayer(theoplayerView)
    var source by rememberSaveable(stateSaver = SourceSaver) { mutableStateOf(sources.first()) }
    var sourceMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(player, source) {
        player.source = source.sourceDescription
        nielsenConnector.updateMetadata(source.nielsenMetadata)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(text = stringResource(R.string.app_name))
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TextButton(onClick = {
                        player.player?.let { it.currentTime -= 10 }
                    }) {
                        Text("-10")
                    }
                    TextButton(onClick = { sourceMenuOpen = true }) {
                        Text("set source")
                    }
                    TextButton(onClick = {
                        if (player.paused) player.play() else player.pause()
                    }) {
                        Text("play/pause")
                    }
                    TextButton(onClick = {
                        player.player?.let { it.currentTime += 10 }
                    }) {
                        Text("+10")
                    }
                }
            }
        }) { padding ->
            DefaultUI(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(1f),
                player = player
            )

            if (sourceMenuOpen) {
                SelectSourceDialog(
                    sources = sources,
                    currentSource = source,
                    onSelectSource = {
                        source = it
                        sourceMenuOpen = false
                    },
                    onDismissRequest = { sourceMenuOpen = false }
                )
            }
        }
    }
}

@Composable
fun SelectSourceDialog(
    sources: List<Source>,
    currentSource: Source,
    onSelectSource: (Source) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select a stream",
                    style = MaterialTheme.typography.headlineSmall
                )
                LazyColumn {
                    items(count = sources.size) { index ->
                        val source = sources[index]
                        ListItem(
                            headlineContent = { Text(text = source.name) },
                            leadingContent = {
                                RadioButton(
                                    selected = (source == currentSource),
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable(onClick = {
                                onSelectSource(source)
                            })
                        )
                    }
                }
            }
        }
    }
}
