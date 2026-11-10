plugins {
    alias(libs.plugins.android.connector.library)
}

android {
    namespace = "com.theoplayer.android.connector.analytics.conviva"

    defaultConfig {
        // The IMA integration requires API 24.
        minSdk = 24
    }

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

dokka {
    moduleName = "Conviva Connector"
}
