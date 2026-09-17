import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import tech.stit.convention.libs

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("stit.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<LibraryExtension> {
            buildFeatures { compose = true }
        }

        val bom = libs.findLibrary("androidx-compose-bom").get()
        dependencies {
            add("implementation", platform(bom))
            add("testImplementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("implementation", libs.findLibrary("androidx-compose-ui").get())
            add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            add("implementation", libs.findLibrary("androidx-compose-foundation").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }
}
