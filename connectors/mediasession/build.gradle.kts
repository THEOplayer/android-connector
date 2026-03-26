import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
    alias(libs.plugins.android.connector.library)
}
apply(from = "$rootDir/connectors/publish.gradle")

android {
    namespace = "com.theoplayer.android.connector.mediasession"
}

dependencies {
    val sdkVersion: String by project.ext

    implementation(libs.androidx.media)
    compileOnly("com.theoplayer.theoplayer-sdk-android:core:$sdkVersion")
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Media Session Connector"
}
