import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.theoplayer.android.connector"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.theoplayer.android.connector"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "player"
    productFlavors {
        create("latestPlayer") {
            // Use the latest supported THEOplayer version
            dimension = "player"
        }
        create("minPlayer") {
            // Use the minimum supported THEOplayer version
            dimension = "player"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_0
        jvmTarget = JvmTarget.JVM_1_8
    }
}

dependencies {
    val latestPlayerImplementation = configurations.getByName("latestPlayerImplementation")
    val minPlayerImplementation = configurations.getByName("minPlayerImplementation")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    latestPlayerImplementation(libs.theoplayer)
    latestPlayerImplementation(libs.theoplayer.integration.ima)
    latestPlayerImplementation(libs.theoplayer.integration.dai)

    minPlayerImplementation(libs.theoplayer) {
        version {
            strictly(libs.versions.theoplayerMin.get())
        }
    }
    minPlayerImplementation(libs.theoplayer.integration.ima) {
        version {
            strictly(libs.versions.theoplayerMin.get())
        }
    }
    minPlayerImplementation(libs.theoplayer.integration.dai) {
        version {
            strictly(libs.versions.theoplayerMin.get())
        }
    }

    implementation(libs.theoplayer.android.ui)

    implementation(project(":connectors:analytics:conviva"))
    implementation(libs.conviva)

    implementation(project(":connectors:analytics:nielsen"))
    implementation(libs.nielsen)

    implementation(project(":connectors:analytics:comscore"))
    implementation(libs.comscore)

    implementation(project(":connectors:yospace"))
    implementation(libs.yospace)

    implementation(project(":connectors:uplynk"))
}
