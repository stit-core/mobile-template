import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import tech.stit.convention.libs

/**
 * Профиль feature-модуля: Compose-библиотека + Hilt + Roborazzi + общие зависимости слоя
 * (designsystem, model, lifecycle). Модуль в своём build.gradle.kts объявляет только namespace
 * и собственные зависимости.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("stit.android.library.compose")
        pluginManager.apply("stit.android.hilt")
        pluginManager.apply("stit.android.roborazzi")

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:model"))
            add("implementation", libs.findLibrary("androidx-compose-material3").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("testImplementation", project(":core:testing"))
        }
    }
}
