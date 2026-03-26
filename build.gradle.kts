import java.time.Year
import kotlin.text.Typography.copyright

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinx.serialization) apply false
}

tasks.register("updateVersion") {
    val versionCatalog = rootProject.file("gradle/libs.versions.toml")
    inputs.file(versionCatalog)

    val sdkVersion: String? by project.ext
    require(!sdkVersion.isNullOrBlank()) { "Missing sdkVersion" }

    ant.withGroovyBuilder {
        "replaceregexp"(
            "file" to versionCatalog,
            "match" to """^theoplayer = \{(.*) prefer = ".+" (.*)\}$""",
            "replace" to """theoplayer = {\1 prefer = "$sdkVersion" \2}""",
            "byline" to true
        )
        "replaceregexp"(
            "file" to versionCatalog,
            "match" to """^androidConnector = ".+"$""",
            "replace" to """androidConnector = "$sdkVersion"""",
            "byline" to true
        )
    }
}

dependencies {
    dokka(project(":connectors:analytics:comscore"))
    dokka(project(":connectors:analytics:conviva"))
    dokka(project(":connectors:analytics:nielsen"))
    dokka(project(":connectors:mediasession"))
    // FIXME Re-enable Yospace connector
    // dokka(project(":connectors:yospace"))
    dokka(project(":connectors:uplynk"))
}

dokka {
    val connectorVersion: String = libs.versions.androidConnector.get()

    moduleName = "THEOplayer Connectors for Android"
    version = connectorVersion

    pluginsConfiguration.html {
        customAssets.from("assets/logo-icon.svg")
        footerMessage =
            "$copyright ${Year.now().value} Dolby Laboratories, Inc. All rights reserved."
    }

    dokkaPublications.html {
        outputDirectory = rootDir.resolve("site/api")
    }
}
