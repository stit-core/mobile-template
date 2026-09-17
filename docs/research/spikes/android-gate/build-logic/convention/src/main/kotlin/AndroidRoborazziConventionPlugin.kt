import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import tech.stit.convention.libs

/** Screenshot-тесты без устройства: Robolectric + Roborazzi в unitTest-таргете. */
class AndroidRoborazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.github.takahirom.roborazzi")

        dependencies {
            add("testImplementation", libs.findLibrary("robolectric").get())
            add("testImplementation", libs.findLibrary("roborazzi").get())
            add("testImplementation", libs.findLibrary("roborazzi-compose").get())
            add("testImplementation", libs.findLibrary("roborazzi-junit-rule").get())
            add("testImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
            add("testImplementation", libs.findLibrary("androidx-test-ext-junit").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
        }
    }
}
