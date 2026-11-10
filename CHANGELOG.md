# Changelog

> **Tags:**
> - 💥 Breaking Change
> - 🚀 New Feature
> - 🐛 Bug Fix
> - 👎 Deprecation
> - 📝 Documentation
> - 🏠 Internal
> - 💅 Polish

## [Unreleased]

## [11.1.3] (2026-08-28)

* 🐛 **Conviva**: Fixed an issue where OptiView Ads ad breaks that failed before an ad became available were not reported as failed ad attempts. ([#108](https://github.com/THEOplayer/android-connector/pull/108))
* 🚀 **Conviva**: Added reporting of ad-break position to Conviva. ([#108](https://github.com/THEOplayer/android-connector/pull/108))
* 📝 **Yospace**: Clarified that the connector provides a Server-Side Ad Insertion (SSAI) integration. ([#93](https://github.com/THEOplayer/android-connector/pull/93))

## [11.1.2] (2026-07-07)

* 🐛 **All connectors**: Fixed dependency compatibility with THEOplayer by reverting the Kotlin compiler to 2.2.10 and AndroidX Core to 1.17.0. ([#102](https://github.com/THEOplayer/android-connector/pull/102))

## [11.1.1] (2026-07-07)

* 🐛 **MediaSession**: Fixed an issue where default playback actions no longer worked when no custom playback callback was set. ([#100](https://github.com/THEOplayer/android-connector/pull/100))
* 🐛 **Yospace**: Fixed an issue where failing to load a non-linear ad image could crash the app. ([#90](https://github.com/THEOplayer/android-connector/pull/90))
* 🐛 **Yospace**: Fixed an issue where stream-start event listeners were not removed correctly. ([#89](https://github.com/THEOplayer/android-connector/pull/89))
* 💥 **Yospace**: Raised the minimum supported Yospace SDK version to 3.10.0 and updated session-property handling for newer SDK versions. ([#89](https://github.com/THEOplayer/android-connector/pull/89))
* 🚀 **Conviva**: Updated the Conviva SDK to 4.0.51. ([#98](https://github.com/THEOplayer/android-connector/pull/98))
* 🚀 **Comscore**: Updated the Comscore SDK to 6.14.0. ([#98](https://github.com/THEOplayer/android-connector/pull/98))

## [11.1.0] (2026-05-07)

* 🐛 **MediaSession**: Fixed an issue where changes to `enabledPlaybackActions` were not applied to the media session's playback state. ([#74](https://github.com/THEOplayer/android-connector/pull/74))
* 🚀 **MediaSession**: Added an optional `PlaybackCallback` to intercept and override playback actions triggered by the media session. ([#74](https://github.com/THEOplayer/android-connector/pull/74))

## [11.0.1] (2026-04-16)

* 💥 **All connectors**: Added support for THEOplayer 11, which is now the minimum supported THEOplayer version. ([#83](https://github.com/THEOplayer/android-connector/pull/83))

## [11.0.0] (2026-04-16)

* 🐛 **Conviva**: Fixed an issue where ad bitrate was reported incorrectly. ([#80](https://github.com/THEOplayer/android-connector/pull/80))
* 🐛 **Conviva**: Fixed an issue where ad event listeners were not removed when destroying the connector. ([#80](https://github.com/THEOplayer/android-connector/pull/80))

[Unreleased]: https://github.com/THEOplayer/android-connector/compare/11.1.3...HEAD
[11.1.3]: https://github.com/THEOplayer/android-connector/compare/11.1.2...11.1.3
[11.1.2]: https://github.com/THEOplayer/android-connector/compare/11.1.1...11.1.2
[11.1.1]: https://github.com/THEOplayer/android-connector/compare/11.1.0...11.1.1
[11.1.0]: https://github.com/THEOplayer/android-connector/compare/11.0.1...11.1.0
[11.0.1]: https://github.com/THEOplayer/android-connector/compare/11.0.0...11.0.1
[11.0.0]: https://github.com/THEOplayer/android-connector/commits/11.0.0
