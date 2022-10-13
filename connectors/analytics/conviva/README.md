# THEOplayer Android SDK Conviva Connector

The Conviva connector provides a Conviva integration for THEOplayer Unified Android SDK.


## Prerequisites

The THEOplayer Android SDK Conviva Connector requires the application to import the THEOplayer Unified Android SDK since the connector relies on its public APIs.
For more details about importing THEOplayer Unified Android SDK check the [documentation](https://docs.theoplayer.com/getting-started/01-sdks/02-android-unified/00-getting-started.md).

For setting up a valid Conviva session, you must have access to a [Conviva developer account](https://pulse.conviva.com/) with access to a debug or production key.


## Installation
After setting up THEOplayer Unified Android SDK, in your **module** level `build.gradle` file add THEOplayer Unified Android SDK Conviva Connector and the Conviva SDK:

```
implementation 'com.theoplayer.android-connector:conviva:+'
implementation 'com.conviva.sdk:conviva-core-sdk:4.0.23'
```


## Usage

### Setting up the Conviva Connector
```kotlin
val theoplayerView: THEOplayerView

private fun setupConviva() {
    val customerKey = "your_conviva_customer_key"
    val gatewayUrl = "your_conviva_debug_gateway_url"

    val settings = HashMap<String, Any>()
    settings[ConvivaSdkConstants.GATEWAY_URL] = gatewayUrl
    settings[ConvivaSdkConstants.LOG_LEVEL] = SystemSettings.LogLevel.DEBUG

    convivaConnector = ConvivaConnector(applicationContext, theoplayerView.player, customerKey, settings)
    convivaConnector?.setViewerId("viewer ID")
}
```

### Setting asset name
Whenever a new source is set on the player, follow it by setting the new asset name.
For example:

```kotlin
theoplayerView.player.source = sourceDescription
convivaConnector?.setAssetName("BigBuckBunny with Google IMA ads")
```

### Destroying / Cleaning up
To release listeners and resources, call destroy whenever the Conviva Connector is no longer needed. 
```kotlin
convivaConnector?.destroy()
```

Note:
* After destroying a Conviva Connector instance, it can no longer be used. If needed, a new instance should be created.
