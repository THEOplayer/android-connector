import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
    alias(libs.plugins.android.connector.library)
    alias(libs.plugins.kotlinx.serialization)
}
apply(from = "$rootDir/connectors/publish.gradle")

android {
    namespace = "com.theoplayer.android.connector.uplynk"
}

dependencies {
    val sdkVersion: String by project.ext

    compileOnly("com.theoplayer.theoplayer-sdk-android:core:$sdkVersion")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation("com.theoplayer.theoplayer-sdk-android:core:$sdkVersion")
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Uplynk Connector"
}