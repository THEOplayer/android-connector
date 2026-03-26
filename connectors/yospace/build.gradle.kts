import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
    alias(libs.plugins.android.connector.library)
    alias(libs.plugins.kotlinx.serialization)
}

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
    implementation(libs.androidx.core.ktx)
    compileOnly(libs.theoplayer)
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
    testImplementation(libs.theoplayer)
    testImplementation(libs.yospace)
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Yospace Connector"

    dokkaSourceSets.named("main") {
        samples.from(project.fileTree("src/test/java/"))
    }
}
