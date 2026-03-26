import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

abstract class AndroidConnectorLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "maven-publish")

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
        }
    }
}