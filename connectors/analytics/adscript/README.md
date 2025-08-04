# Adscript Connector

The Adscript connector provides a [Nielsen Adscript](https://adscript.admosphere.cz/) integration for 
the THEOplayer Android SDK.

## Prerequisites

The Adscript connector requires downloading the private 
[AdScript client library](https://adscript.admosphere.cz/download/AdScriptApiClient_v1.0.10.aar.gz) 
into the app's `libs/` folder. Decompress it and pass the SDK location to the connector 
by setting the `adscriptSdkDir` in your app's `gradle.properties` file:

```bash
# Location of the adscript SDK
adscriptSdkDir=./app/libs/
```

## Usage

Create config and metadata objects, and pass them when building the `AdscriptConnector` instance.  

```kotlin
val config = AdscriptConfiguration(implementationId = "exampleadscript", debug = true)
val metadata = AdScriptDataObject().apply {
    set(AdScriptDataObject.FIELD_assetId, "bbb-example")
    set(AdScriptDataObject.FIELD_type, AdScriptDataObject.OBJ_TYPE_content)
    set(AdScriptDataObject.FIELD_program, "animation")
    set(AdScriptDataObject.FIELD_title, "Big Buck Bunny")
    set(AdScriptDataObject.FIELD_crossId, "1234")
    set(AdScriptDataObject.FIELD_length, "596000")
    set(AdScriptDataObject.FIELD_livestream, "0")
    set(AdScriptDataObject.FIELD_attribute, AdScriptDataObject.ATTRIBUTE_RegularProgram)
}
adscriptConnector = AdscriptConnector(
    activity = this,
    playerView = theoplayerView,
    configuration = config,
    contentMetadata = metadata,
    adProcessor = null)
```

The session needs to be started every time the app resumes, so in `onResume` add:

```kotlin
adscriptConnector.sessionStart()
```