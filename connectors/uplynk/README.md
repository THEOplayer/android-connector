# THEOplayer Uplynk Connector for Android

The Uplynk connector provides a Uplynk integration for THEOplayer.

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
2. Add THEOplayer, the Uplynk Ad Management SDK and the Uplynk Connector as dependencies in your module-level `build.gradle` file:
   ```groovy
   dependencies {
       implementation "com.theoplayer.theoplayer-sdk-android:core:7.+"
       implementation "com.theoplayer.android-connector:uplynk:7.+"
   }
   ```

## Usage

First, follow [the getting started guide for the THEOplayer Android SDK][android-getting-started]
to set up a `THEOplayerView` in your app.

Then, create the `UplynkConnector`:

```kotlin
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.connector.uplynk.UplynkConnector
...
val theoPlayerView = findViewById(R.id.theoplayer)
val uplynkConnector = UplynkConnector(theoPlayerView)
```

Finally, set the THEOplayer source to a `SourceDescription` with a `UplynkSsaiDescription` as its `ssai` description:
```kotlin
theoplayerView.player.source = SourceDescription
    .Builder(
        TypedSource.Builder("no source")
            .ssai(
                UplynkSsaiDescription
                    .Builder()
                    .prefix("https://content.uplynk.com")
                    .assetIds(listOf(
                        "41afc04d34ad4cbd855db52402ef210e",
                        "c6b61470c27d44c4842346980ec2c7bd",
                        "588f9d967643409580aa5dbe136697a1",
                        "b1927a5d5bd9404c85fde75c307c63ad",
                        "7e9932d922e2459bac1599938f12b272",
                        "a4c40e2a8d5b46338b09d7f863049675",
                        "bcf7d78c4ff94c969b2668a6edc64278",
                    ))
                    .preplayParameters(LinkedHashMap(mapOf(
                        "ad" to "adtest",
                        "ad.lib" to "15_sec_spots"
                    )))
                    .build())
            .build()
    )
    .build()
```

[uplynk-documentation]: https://docs.edgecast.com/video/#Setup/Setup-Overview.htm%3FTocPath%3DBasic%2520Setup%7C_____0
[android-getting-started]: https://www.theoplayer.com/docs/theoplayer/getting-started/sdks/android/getting-started/