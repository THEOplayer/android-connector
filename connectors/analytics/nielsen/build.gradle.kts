import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
    alias(libs.plugins.android.connector.library)
}
apply(from = "$rootDir/connectors/publish.gradle")

android {
    namespace = "com.theoplayer.android.connector.analytics.nielsen"
}

dependencies {
    val sdkVersion: String by project.ext

    implementation(libs.androidx.lifecycle.process)

    compileOnly("com.theoplayer.theoplayer-sdk-android:core:$sdkVersion")
    compileOnly("com.theoplayer.theoplayer-sdk-android:integration-ads-ima:$sdkVersion")
    compileOnly(libs.nielsen)
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Nielsen Connector"
}

