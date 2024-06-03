# THEOplayer Yospace Connector for Android

The Yospace connector provides a Yospace integration for THEOplayer.

## Installation

1. Add the THEOplayer Maven repository to your project-level `settings.gradle` file:
   ```groovy
   dependencyResolutionManagement {
       repositories {
           google()
           mavenCentral()
           maven { url = uri("https://maven.theoplayer.com/releases/") }
       }
   }
   ```
2. Add THEOplayer and the Yospace Connector as a dependency in your module-level `build.gradle` file:
   ```groovy
   dependencies {
       implementation "com.theoplayer.theoplayer-sdk-android:core:7.+"
       implementation "com.theoplayer.android-connector:yospace:7.+"
   }
   ```

## Usage

First, follow [the getting started guide for the THEOplayer Android SDK][android-getting-started]
to set up a `THEOplayerView` in your app.

Then, create the `YospaceConnector`:

```kotlin
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.connector.yospace.YospaceConnector

val theoPlayerView = findViewById(R.id.theoplayer)
val yospaceConnector = YospaceConnector(theoPlayerView)
```

Finally, set the THEOplayer source to a `SourceDescription` with a `YospaceSsaiDescription` as its `ssai` description:
```kotlin
theoplayerView.player.source = SourceDescription.Builder(
   TypedSource.Builder("https://example.com/stream.m3u8")
      .ssai(
         YospaceSsaiDescription(streamType = YospaceStreamType.LIVE)
      )
      .build()
).build()
```

[android-getting-started]: https://www.theoplayer.com/docs/theoplayer/getting-started/sdks/android/getting-started/
