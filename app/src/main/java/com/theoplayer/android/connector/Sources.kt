package com.theoplayer.android.connector

import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription
import com.theoplayer.android.api.source.metadata.MetadataDescription

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
        )
    )
}