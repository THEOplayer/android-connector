plugins {
    alias(libs.plugins.android.connector.library)
}

android {
    namespace = "com.theoplayer.android.connector.analytics.nielsen"
}

dependencies {
    implementation(libs.androidx.lifecycle.process)

    compileOnly(libs.theoplayer)
    compileOnly(libs.theoplayer.integration.ima)
    compileOnly(libs.nielsen)
}

dokka {
    moduleName = "Nielsen Connector"
}

