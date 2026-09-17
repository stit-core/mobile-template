import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import tech.stit.convention.library
import tech.stit.convention.libs

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { buildFeatures { compose = true } }
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { buildFeatures { compose = true } }
        }

        // Отчёты стабильности Compose — по флагу: ./gradlew -PcomposeCompilerReports=true assembleDevDebug
        if (providers.gradleProperty("composeCompilerReports").isPresent) {
            extensions.configure<ComposeCompilerGradlePluginExtension> {
                metricsDestination.set(layout.buildDirectory.dir("compose-compiler"))
                reportsDestination.set(layout.buildDirectory.dir("compose-compiler"))
            }
        }

        val bom = libs.library("androidx-compose-bom")
        dependencies {
            add("implementation", platform(bom))
            add("testImplementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("implementation", libs.library("androidx-compose-ui"))
            add("implementation", libs.library("androidx-compose-foundation"))
            add("implementation", libs.library("androidx-compose-material3"))
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
        }
    }
}
