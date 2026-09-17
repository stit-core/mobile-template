import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import tech.stit.convention.library
import tech.stit.convention.libs

// Один профиль качества на каждый модуль (и корень): detekt + compose-rules, Kover, Spotless, порядок зависимостей.
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        // Kover должен стоять на каждом модуле — точка сбора (:androidApp:app) только агрегирует (06 L-09).
        pluginManager.apply("org.jetbrains.kotlinx.kover")
        pluginManager.apply("com.diffplug.spotless")
        pluginManager.apply("com.squareup.sort-dependencies")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            parallel = true
            autoCorrect = false
        }
        // detekt-formatting не подключаем: внутри ktlint 0.50, он спорит со Spotless/ktlint 1.8 (06 L-01).
        dependencies { add("detektPlugins", libs.library("detekt-compose-rules")) }
        // Plain-таск detekt знает только src/main и src/test — KMP source sets shared (commonMain, iosMain…)
        // остались бы без анализа; сгенерированный код (core/api/generated) руками не правится и не судится.
        tasks.named<Detekt>("detekt") {
            setSource(files("src"))
            exclude("**/generated/**")
        }
        tasks.withType<Detekt>().configureEach {
            reports {
                sarif.required.set(true)
                xml.required.set(true)
                txt.required.set(true)
                html.required.set(false)
                md.required.set(false)
            }
        }

        // Единственный автофиксер формата Kotlin; корень дополнительно держит build-logic и корневые *.kts.
        val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
        extensions.configure<SpotlessExtension> {
            kotlin {
                target("src/**/*.kt", "build-logic/convention/src/**/*.kt")
                // Сгенерированные модели API формирует генератор, а не ktlint: gen-check сверяет их байт в байт.
                targetExclude("**/generated/**")
                ktlint(ktlintVersion)
                trimTrailingWhitespace()
                endWithNewline()
            }
            kotlinGradle {
                target("*.kts", "build-logic/**/*.kts")
                targetExclude("**/build/**")
                ktlint(ktlintVersion)
            }
        }
    }
}
