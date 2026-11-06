plugins {
    alias(libs.plugins.android.connector.library)
}

android {
    namespace = "com.theoplayer.android.connector.analytics.lens"
}

dependencies {
    compileOnly(libs.theoplayer)

    // Lens types are part of this connector's API; the context add-on wires the app lifecycle, device class and user ID.
    api(libs.lens.sdk)
    implementation(libs.lens.sdk.context)

    testImplementation(libs.junit)
    testImplementation(libs.theoplayer)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.bytebuddy)
    testImplementation(libs.bytebuddy.agent)
    testImplementation(libs.robolectric)
}

dokka {
    moduleName = "OptiView Lens Connector"
}
