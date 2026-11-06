# THEOplayer Android SDK Lens Connector

The Lens connector reports playback of THEOplayer to [Dolby OptiView Lens](https://optiview.dolby.com/).

The connector implements a Lens player adapter for THEOplayer and joins Lens with it. Lens then reports playback on its
own, as CMCD (CTA-5004-B) event-mode reports: a session per source from the first play request, playback states,
errors, and the player keys it can derive, such as the playhead, buffer length, throughput and bitrates.

## Prerequisites

The THEOplayer Android SDK Lens Connector requires the application to import the THEOplayer Android SDK since the connector relies on its public APIs.
For more details, check out our [Getting started on Android](https://www.theoplayer.com/docs/theoplayer/getting-started/sdks/android/getting-started/) guide.

## Installation

After setting up the THEOplayer Android SDK, in your **module** level `build.gradle` file add the THEOplayer Android SDK
Lens Connector:

```groovy
implementation 'com.theoplayer.android-connector:lens:+'
```

The connector brings the Lens SDK (`com.dolby.optiview:lens-sdk`) and its Android add-on
(`com.dolby.optiview:lens-sdk-context`), which tells Lens about the device and the app moving to the background.
Gradle resolves one copy of the Lens SDK, shared with any other SDK in your app that reports to Lens.

## Usage

### Setting up the Lens Connector

Create one connector per player. It takes the options of `Lens.join`; their `player` is set by the connector:

```kotlin
val theoplayerView: THEOplayerView

private fun setupLens() {
    lensConnector = LensConnector(
        theoplayerView,
        LensJoinOptions(
            namespace = "my_app",
            destinations = listOf(LensDestination("https://collector.example.com/lens", format = LensFormat.JSON))
        )
    )
}
```

`namespace` names your product in Lens: its heartbeats and your own events carry it. The playback reports themselves
are sent in Lens's `lens` namespace, so a destination that filters on namespaces must list `lens` to receive them.

Lens reports to the OptiView ingest by default. To report only to your own destinations, call
`Lens.configure(LensConfigureOptions(LensBackend.DISABLED))` before creating the connector.

### Content ID

Lens derives the content ID (`cid`) from the source URL. To report your own, or to report sources without a URL, such
as Google DAI streams, return it from a `ContentIdProvider`:

```kotlin
lensConnector = LensConnector(theoplayerView, joinOptions) { source -> source.metadata?.get("title") as? String }
```

### Your own events

`lensConnector.handle` is the Lens handle the connector joined with. Use it to emit your own events, to set consent, or
to flush:

```kotlin
lensConnector.handle.emitCustom("chapterStart", LensPayload(custom = mapOf("chapter" to 3)))
```

### Other SDKs

Lens reports one session per player adapter. Another SDK that reports this player to Lens should join with
`lensConnector.adapter`, so that both report into the same session.

### Destroying

The connector ends the session and stops reporting when the player is destroyed, or when you destroy it:

```kotlin
lensConnector.destroy()
```

## Limitations

- THEOplayer exposes the quality being played, not the one being loaded. Lens therefore reports the top and lowest
  bitrates (`tb`/`lb`, or `tab`/`lab` for HLS), and the playhead bitrate (`pb`) outside HLS, but not the encoded
  bitrate (`br`) or bitrate changes (`bc`).
- Ads are not reported as such: Lens sees what the content player does during an ad break.
