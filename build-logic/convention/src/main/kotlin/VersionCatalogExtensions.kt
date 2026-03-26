import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = getVersionCatalog()

private fun Project.getVersionCatalog(name: String = "libs"): VersionCatalog {
    return extensions
        .getByType<VersionCatalogsExtension>()
        .named(name)
}

internal val VersionCatalog.versions: Versions
    get() = Versions(this)

class Versions(catalog: VersionCatalog) {
    val androidConnector: String = catalog.findVersionOrThrow("androidConnector")
}

internal fun VersionCatalog.findVersionOrThrow(name: String): String {
    return findVersion(name)
        .orElseThrow { NoSuchElementException("Version $name not found in version catalog") }
        .requiredVersion
}