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

Create one connector per player:

```kotlin
val theoplayerView: THEOplayerView

private fun setupLens() {
    lensConnector = LensConnector(theoplayerView)
}
```

Lens reports to the OptiView ingest by default. The `LensConfiguration` takes the options of `Lens.join`, except
`namespace` and `player`, for example to add destinations of your own:

```kotlin
lensConnector = LensConnector(
    theoplayerView,
    LensConfiguration(destinations = listOf(LensDestination("https://collector.example.com/lens", format = LensFormat.JSON)))
)
```

To report only to your own destinations, call `Lens.configure(LensConfigureOptions(LensBackend.DISABLED))` before
creating the connector.

The playback reports are sent in Lens's `lens` namespace, and the connector's heartbeats in the `theoplayer` namespace.
A destination that filters on namespaces must list them to receive them.

### Content ID

Lens derives the content ID (`cid`) from the source URL, and the connector names a Google DAI source by its asset key,
or its content source ID and video ID. To report your own, or to report other sources without a URL, return it from a
`ContentIdProvider`:

```kotlin
lensConnector = LensConnector(
    theoplayerView,
    LensConfiguration(contentIdProvider = { source -> source.metadata?.get("title") as? String })
)
```

### Your own events

To report events of your own, join Lens in your own registered namespace with `lensConnector.adapter`. Lens reports one
session per player adapter, so your events and the player's playback share that session. Another SDK that reports this
player to Lens joins with the adapter the same way.

```kotlin
val lens = Lens.join(LensJoinOptions(namespace = "my_namespace", player = lensConnector.adapter))
lens.emitCustom("chapterStart", LensPayload(custom = mapOf("chapter" to 3)))
```

`lensConnector.handle` is the Lens handle the connector joined with. Use it to set consent or to flush.

### Destroying

The connector ends the session and stops reporting when the player is destroyed, or when you destroy it:

```kotlin
lensConnector.destroy()
```

## Demo app

The [demo app](../../../app) reports to Lens through this connector. Its Lens panel shows every report Lens sends,
received in the app itself, and offers:

- **Send to staging**: also sends the reports to the OptiView staging ingest, through `com.dolby.optiview:lens-sdk-dev`.
  Lens locks its backend at the first join, so switching restarts the app. Off, the OptiView backend is disabled.
- **Run scenario**: plays the scenario the Lens connector demos share across platforms, so their reports can be
  compared in the backend. The source reports the content ID `lens-connector-demo-scenario`:
  1. Load Big Buck Bunny (`https://cdn.theoplayer.com/video/big_buck_bunny/big_buck_bunny.m3u8`)
  2. Play 10 s
  3. Pause 3 s
  4. Play 5 s
  5. Seek to 120 s, play 5 s
  6. Mute 3 s, unmute 2 s
  7. Rate x2 5 s, x1 2 s
  8. Picture-in-picture 5 s, inline 2 s
  9. Seek to 5 s before the end, play to the end, wait 2 s
  10. Unload

  Durations count from when the player confirms the step: playing, the seek completed, picture-in-picture entered.
  Picture-in-picture is Android's system one, with the player in THEOplayer's PiP activity: Android has no in-app
  picture-in-picture, where the web demo uses the player's in-page one.

To also send the reports to a receiver on your machine, build with `-PlensReceiverUrl=http://localhost:8787/lens` and
run `adb reverse tcp:8787 tcp:8787`.

## Limitations

- THEOplayer exposes the quality being played, not the one being loaded. Lens therefore reports the top and lowest
  bitrates (`tb`/`lb`, or `tab`/`lab` for HLS), and the playhead bitrate (`pb`) outside HLS, but not the encoded
  bitrate (`br`) or bitrate changes (`bc`).
- Whether the player's view is visible is not reported: THEOplayer Android does not expose it. Lens only sees the app
  moving to the background.
- Whether the buffer is stable is not reported: THEOplayer Android's ready state never reaches `HAVE_ENOUGH_DATA`. Lens
  derives it from the buffer length and the target buffer.
- Fullscreen through [Open Video UI for Android](https://github.com/THEOplayer/android-ui) is not reported: it does not
  go through THEOplayer's presentation mode, which is what the connector observes.
- Only fatal errors are reported.
- Ads are not reported as such: Lens sees what the content player does during an ad break.
