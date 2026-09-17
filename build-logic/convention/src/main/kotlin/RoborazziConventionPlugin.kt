import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import tech.stit.convention.library
import tech.stit.convention.libs

// Скриншот-тесты без устройства: Robolectric + Roborazzi в unitTest-таргете; эталоны — src/test/screenshots.
class RoborazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.github.takahirom.roborazzi")

        dependencies {
            add("testImplementation", libs.library("robolectric"))
            add("testImplementation", libs.library("roborazzi"))
            add("testImplementation", libs.library("roborazzi-compose"))
            add("testImplementation", libs.library("roborazzi-junit-rule"))
            add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
            add("testImplementation", libs.library("androidx-test-ext-junit"))
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
        }
    }
}
