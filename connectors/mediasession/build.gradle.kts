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

dokka {
    moduleName = "Media Session Connector"
}
