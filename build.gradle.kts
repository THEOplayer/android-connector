import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import java.time.Year

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
            "match" to """^theoplayer = ".+"$""",
            "replace" to """theoplayer = "$sdkVersion"""",
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

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

tasks.withType(DokkaMultiModuleTask::class).configureEach {
    moduleName = "THEOplayer Connectors for Android"

    val dokkaBaseConfiguration = """
    {
      "customAssets": ["${file("assets/logo-icon.svg").path.replace("\\", "\\\\")}"],
      "footerMessage": "&copy; ${Year.now().value} THEO Technologies"
    }
    """

    pluginsMapConfiguration = mapOf(
        // fully qualified plugin name to json configuration
        "org.jetbrains.dokka.base.DokkaBase" to dokkaBaseConfiguration
    )
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory = rootProject.file("site/api")
}

val connectorVersion: String = libs.versions.androidConnector.get()

subprojects {
    version = connectorVersion

    tasks.withType(AbstractDokkaLeafTask::class).configureEach {
        suppressObviousFunctions = true
        suppressInheritedMembers = true

        dokkaSourceSets {
            configureEach {
                // Use to include or exclude non public members
                includeNonPublic = false
                // Do not output deprecated members. Applies globally, can be overridden by packageOptions
                skipDeprecated = false
                // Emit warnings about not documented members. Applies globally, also can be overridden by packageOptions
                reportUndocumented = true
                // Do not create index pages for empty packages
                skipEmptyPackages = false
                // Used for linking to JDK documentation
                jdkVersion = 11
                // Use to enable or disable linking to online kotlin-stdlib documentation
                noStdlibLink = false
                // Use to enable or disable linking to online JDK documentation
                noJdkLink = false
                // Use to enable or disable linking to online Android documentation (only applicable for Android projects)
                noAndroidSdkLink = false

                externalDocumentationLink {
                    url = uri("https://www.theoplayer.com/docs/theoplayer/v10/api-reference/android/").toURL()
                    packageListUrl = uri("https://www.theoplayer.com/docs/theoplayer/v10/api-reference/android/package-list").toURL()
                }

                perPackageOption {
                    matchingRegex = "com[.]theoplayer[.]android[.]connector[.].*[.]internal.*"
                    suppress = true
                }
            }
        }
    }
}
