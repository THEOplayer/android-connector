package com.theoplayer.android.connector

import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription
import com.theoplayer.android.api.source.metadata.MetadataDescription
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
        ),
        Source(
            name = "Uplynk Ads",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder(
                        "no source"
                    )
                        .type(SourceType.HLS)
//                        .ssai(UplynkSsaiDescription.Builder("https://content-aeui1.uplynk.com/preplay2/41afc04d34ad4cbd855db52402ef210e,c6b61470c27d44c4842346980ec2c7bd,588f9d967643409580aa5dbe136697a1,b1927a5d5bd9404c85fde75c307c63ad,7e9932d922e2459bac1599938f12b272,a4c40e2a8d5b46338b09d7f863049675,bcf7d78c4ff94c969b2668a6edc64278/2d1e72d308d5572d1d3406ac9235b1c4/4EAmYiYe5vqONOtOimqkGyWYbILhAAyv6UFpw3z68S1.m3u8?pbs=d63cc372b741400e868da39af9690916").build())
                        .ssai(UplynkSsaiDescription.Builder("https://content.uplynk.com/preplay/41afc04d34ad4cbd855db52402ef210e,c6b61470c27d44c4842346980ec2c7bd,588f9d967643409580aa5dbe136697a1,b1927a5d5bd9404c85fde75c307c63ad,7e9932d922e2459bac1599938f12b272,a4c40e2a8d5b46338b09d7f863049675,bcf7d78c4ff94c969b2668a6edc64278/multiple.json?v=2&ad=adtest&ad.lib=15_sec_spots").build())
                        .build()
                )
                .build()
        )
    )
}