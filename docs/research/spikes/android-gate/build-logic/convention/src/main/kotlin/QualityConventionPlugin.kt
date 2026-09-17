import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import tech.stit.convention.libs

/** Один профиль качества на все модули: detekt + compose-rules + formatting. */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        // kover должен стоять на каждом модуле — корень только агрегирует
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            parallel = true
            autoCorrect = false
        }

        // detekt-formatting НЕ подключаем: он несёт ktlint 0.50.0 и конфликтует
        // со Spotless/ktlint 1.8.0. Форматирование — одна инстанция, Spotless.
        dependencies {
            add("detektPlugins", libs.findLibrary("detekt-compose-rules").get())
        }

        tasks.withType<Detekt>().configureEach {
            reports {
                sarif.required.set(true)
                xml.required.set(true)
                html.required.set(false)
                txt.required.set(true)
                md.required.set(false)
            }
        }
    }
}
