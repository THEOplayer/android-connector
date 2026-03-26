import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    id("android-connector.library-conventions")
}
apply(from = "$rootDir/connectors/publish.gradle")

android {
    namespace = "com.theoplayer.android.connector.analytics.conviva"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        compilerOptions {
            apiVersion = KotlinVersion.KOTLIN_2_0
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    val sdkVersion: String by project.ext

    compileOnly("com.theoplayer.theoplayer-sdk-android:core:$sdkVersion")
    compileOnly("com.theoplayer.theoplayer-sdk-android:integration-ads-ima:$sdkVersion")
    compileOnly("com.theoplayer.theoplayer-sdk-android:integration-ads-theoads:$sdkVersion")
    compileOnly(libs.conviva)

    implementation(libs.androidx.lifecycle.process)
}

tasks.withType(AbstractDokkaLeafTask::class).configureEach {
    moduleName = "Conviva Connector"
}
