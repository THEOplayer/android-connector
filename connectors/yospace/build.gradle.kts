import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
    alias(libs.plugins.android.connector.library)
    alias(libs.plugins.kotlinx.serialization)
}
apply(from = "$rootDir/connectors/publish.gradle")

android {
    namespace = "com.theoplayer.android.connector.yospace"

    defaultConfig {
        val connectorVersion: String by project.ext
        buildConfigField("String", "LIBRARY_VERSION", "\"${connectorVersion}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    val sdkVersion: String by project.ext

    implementation(libs.androidx.core.ktx)
    compileOnly("com.theoplayer.theoplayer-sdk-android:core:$sdkVersion")
    compileOnly(libs.yospace) {
        version {
            strictly("[3.6, 4.0)")
            prefer(libs.yospace.get().version!!)
        }
    }
    implementation(libs.kotlinx.serialization.json)

    // Tests
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation("com.theoplayer.theoplayer-sdk-android:core:$sdkVersion")
    testImplementation(libs.yospace)
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Yospace Connector"

    dokkaSourceSets.named("main") {
        samples.from(project.fileTree("src/test/java/"))
    }
}
