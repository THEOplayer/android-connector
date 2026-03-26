import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
    alias(libs.plugins.android.connector.library)
}
apply(from = "$rootDir/connectors/publish.gradle")

android {
    namespace = "com.theoplayer.android.connector.analytics.conviva"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    compileOnly(libs.theoplayer)
    compileOnly(libs.theoplayer.integration.ima)
    compileOnly(libs.theoplayer.integration.theoads)
    compileOnly(libs.conviva)

    implementation(libs.androidx.lifecycle.process)
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Conviva Connector"
}
