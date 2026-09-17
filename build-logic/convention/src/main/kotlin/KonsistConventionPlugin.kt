import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import tech.stit.convention.library
import tech.stit.convention.libs

// Архитектурные тесты Konsist: обычные JUnit-тесты, падают в стадии «android unit tests» гейта.
class KonsistConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        dependencies {
            add("testImplementation", libs.library("konsist"))
            add("testImplementation", libs.library("junit4"))
        }

        // Konsist читает исходники всего репозитория с диска, а не classpath: без этих inputs
        // тест остаётся UP-TO-DATE после нарушения в другом модуле.
        tasks.withType<Test>().configureEach {
            inputs
                .files(
                    rootProject.fileTree("androidApp") {
                        include("**/*.kt")
                        exclude("**/build/**")
                    },
                    rootProject.fileTree("shared/src") { include("**/*.kt") },
                )
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
    }
}
