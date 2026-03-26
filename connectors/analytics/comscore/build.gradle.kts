import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
    alias(libs.plugins.android.connector.library)
}
apply(from = "$rootDir/connectors/publish.gradle")

android {
    namespace = "com.theoplayer.android.connector.analytics.comscore"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    val sdkVersion: String by project.ext

    compileOnly("com.theoplayer.theoplayer-sdk-android:core:$sdkVersion")
    compileOnly("com.theoplayer.theoplayer-sdk-android:integration-ads-ima:$sdkVersion")

    compileOnly(libs.comscore)
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Comscore Connector"
}
