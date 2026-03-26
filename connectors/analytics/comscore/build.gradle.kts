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
    compileOnly(libs.theoplayer)
    compileOnly(libs.theoplayer.integration.ima)

    compileOnly(libs.comscore)
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Comscore Connector"
}
