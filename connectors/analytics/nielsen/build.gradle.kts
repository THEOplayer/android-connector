import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
    alias(libs.plugins.android.connector.library)
}
apply(from = "$rootDir/connectors/publish.gradle")

android {
    namespace = "com.theoplayer.android.connector.analytics.nielsen"
}

dependencies {
    implementation(libs.androidx.lifecycle.process)

    compileOnly(libs.theoplayer)
    compileOnly(libs.theoplayer.integration.ima)
    compileOnly(libs.nielsen)
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Nielsen Connector"
}

