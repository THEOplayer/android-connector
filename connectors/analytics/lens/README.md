# THEOplayer Android SDK OptiView Lens Connector

The OptiView Lens connector reports THEOplayer Android SDK playback to [OptiView Lens](https://optiview.dolby.com/).

Once connected, Lens observes the player on its own: it starts a session at the first play request on each source,
ends it when the source changes or the player is destroyed, and reports playback states, errors, bitrates, buffer levels
and the other player keys it can derive, following CMCD (CTA-5004-B).

## Prerequisites

The THEOplayer Android SDK OptiView Lens Connector requires the application to import the THEOplayer Android SDK since the connector relies on its public APIs.
For more details, check out our [Getting started on Android](https://www.theoplayer.com/docs/theoplayer/getting-started/sdks/android/getting-started/) guide.

## Installation

After setting up the THEOplayer Android SDK, in your **module** level `build.gradle` file add the THEOplayer Android SDK
OptiView Lens Connector:

```groovy
implementation 'com.theoplayer.android-connector:lens:+'
```

The connector brings the OptiView Lens SDK (`com.dolby.optiview:lens-sdk`) and its Android add-on
(`com.dolby.optiview:lens-sdk-context`), which tells Lens about the device and the app moving to the background,
from the THEOplayer Maven repository:

```groovy
repositories {
    maven { url 'https://maven.theoplayer.com/releases/' }
}
```

## Usage

### Setting up the OptiView Lens Connector

```kotlin
val theoplayerView: THEOplayerView

private fun setupLens() {
    lensConnector = LensConnector(theoplayerView, LensJoinOptions(namespace = "my-app"))
}
```

`LensJoinOptions` is the Lens SDK's own configuration: the `namespace` names your application, and optional
`destinations` send the same reports to your own endpoints as well. Its `player` is set by the connector.

The player's reports go to the `lens` namespace. Use the connector's `handle` to report your application's own events
into the player's session:

```kotlin
lensConnector.handle.emitCustom("chapterSelected", LensPayload(custom = mapOf("chapter" to 3)))
```

### Sharing the player with other SDKs

Other SDKs that report to Lens for the same player, such as OptiView Ads, should join with the connector's
`playerAdapter`, so that all reports go into one session:

```kotlin
Lens.join(LensJoinOptions(namespace = "ads", player = lensConnector.playerAdapter))
```

### Destroying / Cleaning up

To release listeners and resources, call destroy whenever the OptiView Lens Connector is no longer needed.

```kotlin
lensConnector.destroy()
```

Notes:
* After destroying an OptiView Lens Connector instance, it can no longer be used. If needed, a new instance should be created.
* Do not configure THEOplayer's own CMCD reporting for a player that Lens observes.
