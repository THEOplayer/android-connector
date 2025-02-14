package com.theoplayer.android.connector

import android.net.Uri
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription
import com.theoplayer.android.api.source.metadata.MetadataDescription
import com.theoplayer.android.connector.uplynk.UplynkAssetType
import com.theoplayer.android.connector.uplynk.UplynkPingConfiguration
import com.theoplayer.android.connector.uplynk.UplynkSsaiDescription
import java.net.URL

data class Source(
    val name: String,
    val sourceDescription: SourceDescription,
    val nielsenMetadata: HashMap<String, Any> = hashMapOf()
)

val backend: List<String> by lazy {
    listOf("Default", "Media3")
}

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
        ),
        Source(
            name = "Uplynk Dynamic",
            sourceDescription = SourceDescription
                .Builder(
                    TypedSource.Builder("no source")
                        .ssai(
                            UplynkSsaiDescription
                                .Builder()
                                .prefix("https://content.uplynk.com")
                                .assetInfo(true)
                                .assetIds(listOf("e86b14f27785468f97c46cf3ef162164"))
                                .build()
                        )
                        .build()
                )
                .build()
        ),
    )
}

data class WebSources(
    val name: String,
    val url: URL,
    val assetID: String,
    val externalID: String?,
    val userID: String?,
    val tokenRequired: Boolean
) {
    companion object {
        // VOD sources:

        // Token required
        val source1 = WebSources(
            name = "Web VOD1 - token required",
            url = URL("https://content.uplynk.com/player5/3D0PYRlstKHws59xA190jfsa.html"),
            assetID = "a96defe30d7543c2bc52097ceb224384",
            externalID = "0x3lzrXaqky_t3qwa9F66w",
            userID = "5d8e9ef63a204d0b8cb71b50093bde7d",
            tokenRequired = true
        )

        val source2 = WebSources(
            name = "Web VOD2 - token required",
            url = URL("https://content.uplynk.com/player5/9cEt0pA4TRvcbBqZyQrgJsa.html"),
            assetID = "d4df519c558341748ad0b36e5f67f906",
            externalID = "QWhv8yi7M0ywkwKW2ehaCA",
            userID = "5d8e9ef63a204d0b8cb71b50093bde7d",
            tokenRequired = true
        )

        val source3 = WebSources(
            name = "Web VOD3 - token required",
            url = URL("https://content.uplynk.com/player5/wGiZcf4hmYRbbrNwfj1Nksa.html"),
            assetID = "e3a379ba6ac04bc897af37fe94db6321",
            externalID = "ux4ELy_Kuk2UldEDJVjI6w",
            userID = "5d8e9ef63a204d0b8cb71b50093bde7d",
            tokenRequired = true
        )

        // Token required + ads
        val source4 = WebSources(
            name = "Web VOD4 - token + Ads",
            url = URL("https://content.uplynk.com/player5/61VhTlJSFS8n48FVANrHzJsa.html"),
            assetID = "4acdbbc618564ae7a6748f23af6f7a3c",
            externalID = null,
            userID = null,
            tokenRequired = true
        )

        // Token is not required
        val source5 = WebSources(
            name = "Web5 - token not required",
            url = URL("https://content.uplynk.com/player5/1W8JSQrGnsk7YtzALXCUPzsa.html"),
            assetID = "e86b14f27785468f97c46cf3ef162164",
            externalID = null,
            userID = null,
            tokenRequired = false
        )

        // LIVE sources:
        val source6 = WebSources(
            name = "Web Live",
            url = URL("https://content.uplynk.com/player5/607exUNBJDf260nEJrrxtRsa.html"),
            assetID = "cbf7d83f86d14a64b1df75386d5c4536",
            externalID = null,
            userID = null,
            tokenRequired = true
        )

        fun build(): List<Source> {
            val sourceList = listOf(source1, source2, source3, source4, source5, source6)
            val sources = sourceList.map {
                val parameters = LinkedHashMap<String, String>()

                if (it.tokenRequired) {
                    val response = requestStreams(it.url.toString())
                    val regex = """let playbackUrl = "(.+?)";""".toRegex()

                    val playbackURLString = regex.find(response)?.groupValues?.get(1)

                    val uri = Uri.parse(playbackURLString)

                    uri.queryParameterNames.map {
                        parameters.put(it, uri.getQueryParameter(it)!!)
                    }
                }

                val ssai = if (it.externalID == null) {
                    UplynkSsaiDescription
                        .Builder()
                        .prefix("https://content.uplynk.com")
                        .assetInfo(true)
                        .assetIds(listOf(it.assetID))
                        .contentProtected(false)
                        .playbackUrlParameters(parameters)
                        .build()
                } else {
                    UplynkSsaiDescription
                        .Builder()
                        .prefix("https://content.uplynk.com")
                        .assetInfo(true)
                        .externalIds(listOf(it.externalID))
                        .userId(it.userID!!)
                        .contentProtected(false)
                        .playbackUrlParameters(parameters)
                        .build()
                }

                Source(
                    name = it.name,
                    sourceDescription = SourceDescription
                        .Builder(
                            TypedSource.Builder("no source")
                                .ssai(ssai)
                                .build()
                        )
                        .build()
                )
            }
            return sources
        }
    }
}
