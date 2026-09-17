import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import tech.stit.convention.library
import tech.stit.convention.libs

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")

        dependencies {
            add("implementation", libs.library("hilt-android"))
            add("ksp", libs.library("hilt-compiler"))
            // Robolectric-тесты с @HiltAndroidTest требуют кодогенерации и в test-таргете.
            add("kspTest", libs.library("hilt-compiler"))
            add("testImplementation", libs.library("hilt-android-testing"))
        }

        // kspTest порождает test-классы даже в модуле без тестов — Gradle 9 счёл бы это «тесты не найдены».
        tasks.withType<Test>().configureEach { failOnNoDiscoveredTests.set(false) }
    }
}
