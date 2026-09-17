import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import tech.stit.convention.library
import tech.stit.convention.libs

// Профиль feature-модуля: Compose-библиотека + Hilt + Roborazzi + слой зависимостей 08 §2.6.
// Модуль в своём build.gradle.kts объявляет только namespace и собственные зависимости.
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("stit.android.library")
        pluginManager.apply("stit.android.compose")
        pluginManager.apply("stit.android.hilt")
        pluginManager.apply("stit.roborazzi")

        dependencies {
            add("implementation", project(":shared"))
            add("implementation", project(":androidApp:core:designsystem"))
            add("implementation", project(":androidApp:core:ui"))
            add("implementation", project(":androidApp:core:navigation"))
            add("implementation", project(":androidApp:core:platform"))
            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-navigation3-runtime"))
            add("testImplementation", project(":androidApp:core:testing"))
        }

        // Фича не зависит от фичи: общее поднимается в core или shared. Ловится уже на конфигурации.
        afterEvaluate {
            configurations
                .filter { it.name == "implementation" || it.name == "api" }
                .flatMap { it.dependencies.withType<ProjectDependency>() }
                .filter { it.path.startsWith(":androidApp:feature:") }
                .forEach { error("$path зависит от ${it.path}: feature-модули не зависят друг от друга (08 §2.6)") }
        }
    }
}
