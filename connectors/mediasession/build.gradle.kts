import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
    alias(libs.plugins.android.connector.library)
}

android {
    namespace = "com.theoplayer.android.connector.mediasession"
}

dependencies {
    implementation(libs.androidx.media)
    compileOnly(libs.theoplayer)
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Media Session Connector"
}
