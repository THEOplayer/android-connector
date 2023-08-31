# THEOplayer Android SDK Conviva Connector

The Conviva connector provides a Conviva integration for THEOplayer Android SDK.

## Prerequisites

The THEOplayer Android SDK Conviva Connector requires the application to import the THEOplayer Android SDK (either WebView-based or Unified Android SDK) since the connector relies on its public APIs. For more details, check out our [Getting started on Unified Android](https://docs.theoplayer.com/getting-started/01-sdks/02-android-unified/00-getting-started.md) or [Getting started on Android](https://docs.theoplayer.com/getting-started/01-sdks/02-android/00-getting-started.md) guide.

For setting up a valid Conviva session, you must have access to a [Conviva developer account](https://pulse.conviva.com/) with access to a debug or production key.

## Installation
After setting up the THEOplayer Android SDK, in your **module** level `build.gradle` file add the THEOplayer Android SDK Conviva Connector and the Conviva SDK:

```
implementation 'com.theoplayer.android-connector:conviva:+'
implementation 'com.conviva.sdk:conviva-core-sdk:4.0.33'
```

## Usage

### Setting up the Conviva Connector
```kotlin
val theoplayerView: THEOplayerView

private fun setupConviva() {
    val customerKey = "your_conviva_customer_key"
    val gatewayUrl = "your_conviva_debug_gateway_url"
    
    val metadata = hashMapOf(
        "Conviva.applicationName" to "THEOplayer",
        "Conviva.viewerId" to "viewerId"
    )
    val config = ConvivaConfiguration(
        customerKey,
        true, // debug
        gatewayUrl,
    )
    convivaConnector = ConvivaConnector(applicationContext, theoplayerView.player, metadata, config)
}
```

### Setting asset metadata

Most media related properties, such as its streamURL, duration or whether it is a live or vod
stream, are determined and passed by the connector itself when setting a new source.

Whenever a new source is set on the player, the metadata `title` property is used to pass an asset name.

```kotlin
theoplayerView.player.source = 
    SourceDescription.Builder(
        TypedSource.Builder("https://cdn.theoplayer.com/video/big_buck_bunny/big_buck_bunny.m3u8").build()
    )
    .metadata(MetadataDescription(mapOf("title" to "Big Buck Bunny")))
    .build()
```

Alternatively, the asset name can be passed to the connector at any time along with additional metadata
through an open key-value map, for example:

```kotlin
theoplayerView.player.source = sourceDescription
convivaConnector?.setContentInfo(hashMapOf(
    "Conviva.assetName" to "Big Buck Bunny",
    "customTag1" to "customValue1",
    "customTag2" to "customValue2"
))
```

### Starting a new session on program boundaries

By default, new sessions are only started on play-out of a new source, or when an ad break starts.
During a live stream it is possible to manually mark the start of a new session, 
for example when a new program starts.

```kotlin
convivaConnector?.stopAndStartNewSession(hashMapOf(
    "Conviva.assetName" to "New program",
    "customTag1" to "customValue1",
    "customTag2" to "customValue2"
))
```

### Destroying / Cleaning up
To release listeners and resources, call destroy whenever the Conviva Connector is no longer needed. 
```kotlin
convivaConnector?.destroy()
```

Note:
* After destroying a Conviva Connector instance, it can no longer be used. If needed, a new instance should be created.
