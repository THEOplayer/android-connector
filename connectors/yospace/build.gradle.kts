plugins {
    alias(libs.plugins.android.connector.library)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.theoplayer.android.connector.yospace"

    defaultConfig {
        val connectorVersion = libs.versions.androidConnector.get()
        buildConfigField("String", "LIBRARY_VERSION", "\"${connectorVersion}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    compileOnly(libs.theoplayer)
    compileOnly(libs.yospace)
    implementation(libs.kotlinx.serialization.json)

    // Tests
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.theoplayer)
    testImplementation(libs.yospace)
}

dokka {
    moduleName = "Yospace Connector"

    dokkaSourceSets.configureEach {
        samples.from(project.fileTree("src/test/java/"))
    }
}
