plugins {
    alias(libs.plugins.android.connector.library)
}

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

dokka {
    moduleName = "Comscore Connector"
}
