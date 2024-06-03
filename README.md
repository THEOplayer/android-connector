# THEOplayer Android SDK Connectors

This repository is maintained by [THEO Technologies](https://www.theoplayer.com/) and contains the different connectors available with THEOplayer Unified Android SDK.

The THEOplayer Unified Android SDK enables you to quickly deliver content playback on Android, Android TV and Fire TV.

Using the available connectors allows you to augment the features delivered through the Unified Android SDK. 

## Prerequisites

The THEOplayer Android SDK Connectors requires the application to import the THEOplayer Unified Android SDK since the connector relies on its public APIs.
For more details about importing THEOplayer Unified Android SDK check the [documentation](https://docs.theoplayer.com/getting-started/01-sdks/02-android-unified/00-getting-started.md).

## Available Connectors

| Connector    | Dependency       | Supported From |                      Documentation                       |
|:-------------|:-----------------|:--------------:|:--------------------------------------------------------:|
| Comscore     | `comscore:+`     |     6.10.0     | [documentation](connectors/analytics/comscore/README.md) |
| Conviva      | `conviva:+`      |     4.1.0      | [documentation](connectors/analytics/conviva/README.md)  |
| Nielsen      | `nielsen:+`      |     5.5.0      | [documentation](connectors/analytics/nielsen/README.md)  |
| MediaSession | `mediasession:+` |     4.8.0      |    [documentation](connectors/mediasession/README.md)    |

Notes:
* The `+` will fetch the latest released version of THEOplayer Android SDK Connector.
* Android Studio will recommend replacing the `+` with the exact version of THEOplayer Android SDK Connector.
* The THEOplayer Unified Android SDK and the THEOplayer Android SDK Connectors are stable when using the same version.
It's not recommended to use different versions for Unified Android SDK and the Connectors.

## Installation

The THEOplayer Android SDK Connectors are available through the [THEOplayer Maven repository](https://maven.theoplayer.com/) using the `com.theoplayer.android-connector` group which is different than the group of THEOplayer Android SDK.

To set up the dependency follow these steps:

In your **project** level `build.gradle` file add the THEOplayer Maven repository:

```
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://maven.theoplayer.com/releases/' }
    }
}
```

In your **module** level `build.gradle` file add one or more of THEOplayer Android SDK Connector, for example:

```
implementation 'com.theoplayer.android-connector:conviva:+'
```

## License

The contents of this package are subject to the [THEOplayer license](https://www.theoplayer.com/terms).
