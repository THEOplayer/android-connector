package com.theoplayer.android.connector

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription
import com.theoplayer.android.api.source.metadata.MetadataDescription
import com.theoplayer.android.connector.uplynk.UplynkAssetType
import com.theoplayer.android.connector.uplynk.UplynkPingConfiguration
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import com.theoplayer.android.connector.yospace.YospaceSsaiDescription
import com.theoplayer.android.connector.yospace.YospaceStreamType

data class Source(
    val name: String,
    val sourceDescription: SourceDescription,
    val nielsenMetadata: HashMap<String, Any> = hashMapOf()
)

val sources: List<Source> by lazy {
    listOf(
        Source(
            name = "BigBuckBunny with Google IMA ads",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder("https://cdn.theoplayer.com/video/big_buck_bunny/big_buck_bunny.m3u8")
                        .build()
                )
                .ads(
                    GoogleImaAdDescription.Builder("https://cdn.theoplayer.com/demos/ads/vast/dfp-linear-inline-no-skip.xml")
                        .timeOffset("5")
                        .build()
                )
                .metadata(MetadataDescription(mutableMapOf("title" to "BigBuckBunny with Google IMA ads")))
                .build(),
            nielsenMetadata = hashMapOf(
                "assetid" to "C112233",
                "program" to "BigBuckBunny with Google IMA ads"
            )
        ),
        Source(
            name = "Yospace HLS VOD",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder(
                        "https://csm-e-sdk-validation.bln1.yospace.com/csm/access/156611618/c2FtcGxlL21hc3Rlci5tM3U4?yo.av=4"
                    )
                        .type(SourceType.HLS)
                        .ssai(YospaceSsaiDescription(streamType = YospaceStreamType.VOD))
                        .build()
                )
                .build()
        ),
        Source(
            name = "Yospace HLS Live",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder(
                        "https://csm-e-sdk-validation.bln1.yospace.com/csm/extlive/yospace02,hlssample42.m3u8?yo.br=true&yo.av=4"
                    )
                        .type(SourceType.HLS)
                        .ssai(YospaceSsaiDescription(streamType = YospaceStreamType.LIVE))
                        .build()
                )
                .build()
        ),
        Source(
            name = "Yospace HLS DVRLive",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder(
                        "https://csm-e-sdk-validation.bln1.yospace.com/csm/extlive/yospace02,hlssample42.m3u8?yo.br=true&yo.lp=true&yo.av=4"
                    )
                        .type(SourceType.HLS)
                        .ssai(YospaceSsaiDescription(streamType = YospaceStreamType.LIVEPAUSE))
                        .build()
                )
                .build()
        ),
        Source(
            name = "Yospace DASH Live",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder(
                        "https://csm-e-sdk-validation.bln1.yospace.com/csm/extlive/yosdk01,t2-dash.mpd?yo.br=true&yo.av=4"
                    )
                        .type(SourceType.DASH)
                        .ssai(YospaceSsaiDescription(streamType = YospaceStreamType.LIVE))
                        .build()
                )
                .build()
        ),
        Source(
            name = "Yospace DASH DVRLive",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder(
                        "https://csm-e-sdk-validation.bln1.yospace.com/csm/extlive/yosdk01,dash.mpd?yo.br=true&yo.lp=true&yo.jt=1000&yo.av=4"
                    )
                        .type(SourceType.DASH)
                        .ssai(YospaceSsaiDescription(streamType = YospaceStreamType.LIVEPAUSE))
                        .build()
                )
                .build()
        ),
        Source(
            name = "Uplynk Ads",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder("no source")
                        .ssai(
                            UplynkSsaiDescription
                                .Builder()
                                .prefix("https://content.uplynk.com")
                                .assetInfo(true)
                                .assetIds(
                                    listOf(
                                        "41afc04d34ad4cbd855db52402ef210e",
                                        "c6b61470c27d44c4842346980ec2c7bd",
                                        "588f9d967643409580aa5dbe136697a1",
                                        "b1927a5d5bd9404c85fde75c307c63ad",
                                        "7e9932d922e2459bac1599938f12b272",
                                        "a4c40e2a8d5b46338b09d7f863049675",
                                        "bcf7d78c4ff94c969b2668a6edc64278",
                                    )
                                )
                                .preplayParameters(
                                    linkedMapOf(
                                        "ad" to "adtest",
                                        "ad.lib" to "15_sec_spots"
                                    )
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        ),
        Source(
            name = "Uplynk Live",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder("no source")
                        .ssai(
                            UplynkSsaiDescription
                                .Builder()
                                .prefix("https://content.uplynk.com")
                                .assetInfo(false)
                                .assetType(UplynkAssetType.CHANNEL)
                                .assetIds(
                                    listOf(
                                        "3c367669a83b4cdab20cceefac253684",
                                    )
                                )
                                .preplayParameters(
                                    linkedMapOf(
                                        "ad" to "cleardashnew",
                                    )
                                )
                                .pingConfiguration(
                                    UplynkPingConfiguration.Builder()
                                        .linearAdData(true)
                                        .adImpressions(false)
                                        .freeWheelVideoViews(false)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        ),
        Source(
            name = "Uplynk DRM",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder("no source")
                        .ssai(
                            UplynkSsaiDescription
                                .Builder()
                                .prefix("https://content.uplynk.com")
                                .assetInfo(true)
                                .assetIds(
                                    listOf(
                                        "e973a509e67241e3aa368730130a104d",
                                        "e70a708265b94a3fa6716666994d877d",
                                    )
                                )
                                .contentProtected(true)
                                .build()
                        )
                        .build()
                )
                .build()
        )
    )
}

object SourceSaver : Saver<Source, Int> {
    override fun restore(value: Int): Source? = sources.getOrNull(value)
    override fun SaverScope.save(value: Source): Int = sources.indexOf(value)
}
