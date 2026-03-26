import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

class AndroidConnectorLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "com.android.library")
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "maven-publish")

        extensions.configure<LibraryExtension> {
            compileSdk = 36

            defaultConfig {
                minSdk = 23
                consumerProguardFiles(file("consumer-rules.pro"))
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        file("proguard-rules.pro")
                    )
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }

            buildFeatures {
                buildConfig = true
            }

            publishing {
                singleVariant("release") {
                    withSourcesJar()
                    // We use Dokka for JavaDoc generation, see dokkaJavadocJar below
                    // withJavadocJar()
                }
            }
        }

        extensions.configure<KotlinAndroidExtension> {
            compilerOptions {
                apiVersion.set(KotlinVersion.KOTLIN_2_0)
                jvmTarget.set(JvmTarget.JVM_1_8)
            }
        }

        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "reposilite"
                    url = uri("https://maven.theoplayer.com/releases")
                    credentials {
                        username = System.getenv("REPOSILITE_USERNAME")
                        password = System.getenv("REPOSILITE_PASSWORD")
                    }
                }
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/THEOplayer/android-connector")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }

            afterEvaluate {
                publications {
                    register<MavenPublication>("release") {
                        groupId = "com.theoplayer.android-connector"
                        artifactId = project.name
                        version = libs.versions.androidConnector
                        from(components.getByName("release"))
                    }
                }
            }
        }
    }
}