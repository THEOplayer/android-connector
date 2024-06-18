package com.theoplayer.android.connector

import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription
import com.theoplayer.android.api.source.metadata.MetadataDescription
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
                .metadata(MetadataDescription(mapOf("title" to "BigBuckBunny with Google IMA ads")))
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
        )
    )
}