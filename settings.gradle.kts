pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.theoplayer.com/releases/") }
        maven { url = uri("https://raw.githubusercontent.com/NielsenDigitalSDK/nielsenappsdk-android/master/") }
        maven {
            url = uri("https://yospacerepo.jfrog.io/yospacerepo/android-sdk")
            credentials {
                username = System.getenv("YOSPACE_USERNAME")
                password = System.getenv("YOSPACE_PASSWORD")
            }
        }
    }
}

rootProject.name = "THEOplayer Connector"
include(":app")
include(":connectors:analytics:comscore")
include(":connectors:analytics:conviva")
include(":connectors:analytics:nielsen")
include(":connectors:mediasession")
// FIXME Re-enable Yospace connector after updating JFrog credentials
// include(":connectors:yospace")
include(":connectors:uplynk")
