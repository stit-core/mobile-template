import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.ExperimentalBCVApi
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import tech.stit.convention.configureLint
import tech.stit.convention.int
import tech.stit.convention.libs

// KMP-ядро: klib под iOS собирается на Linux (без cinterop), логика тестируется через jvmTest.
class KmpLibraryConventionPlugin : Plugin<Project> {
    @OptIn(ExperimentalBCVApi::class)
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")
        pluginManager.apply("stit.quality")

        extensions.configure<KotlinMultiplatformExtension> {
            // Тот же байткод, что у Android-модулей (JVM 17); JDK ставит foojay-резолвер.
            jvmToolchain(17)
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                // У KMP-таргета нет compileSdkMinor: минорный уровень (37.2) задаётся спецификацией.
                compileSdk { version = release(libs.int("compileSdk")) { minorApiLevel = libs.int("compileSdkMinor") } }
                minSdk = libs.int("minSdk")
                // commonTest на Android-таргете не запускается без host-тестов (06 §5).
                withHostTest {}
                lint { configureLint(this) }
            }
            jvm()
            listOf(iosArm64(), iosSimulatorArm64()).forEach { ios ->
                ios.binaries.framework {
                    baseName = "Shared"
                    isStatic = true
                }
            }
            compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
            sourceSets.getByName("commonTest").dependencies { implementation(kotlin("test")) }
        }

        // apiDump/apiCheck: JVM-дамп + klib-дамп; изменение фасада для Swift — осознанный make api-dump.
        // На Linux Apple-таргеты в дампе не валидируются строго; check-mac включает строгий режим флагом.
        extensions.configure<ApiValidationExtension> {
            klib {
                enabled = true
                strictValidation = providers.gradleProperty("stit.klibStrict").isPresent
            }
        }

        // Пол покрытия общей логики — 80 % строк (08 §4.8); сгенерированные модели и сериализаторы вне счёта.
        extensions.configure<KoverProjectExtension> {
            reports {
                total {
                    verify { rule { minBound(80) } }
                    filters { excludes { classes("*.core.api.generated.*", "*\$serializer") } }
                }
            }
        }
    }
}
