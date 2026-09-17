import com.android.build.api.dsl.ApplicationExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import tech.stit.convention.ENV_DIMENSION
import tech.stit.convention.GATE_VARIANT
import tech.stit.convention.configureJava
import tech.stit.convention.configureKoverGateVariant
import tech.stit.convention.configureReleaseSigning
import tech.stit.convention.configureUnitTests
import tech.stit.convention.envFlavors
import tech.stit.convention.int
import tech.stit.convention.libs

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("stit.android.lint")
        pluginManager.apply("stit.quality")

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.int("compileSdk")
            compileSdkMinor = libs.int("compileSdkMinor")
            defaultConfig {
                minSdk = libs.int("minSdk")
                targetSdk = libs.int("targetSdk")
                // Версия приходит из тега (scripts/tools/version-from-tag.sh); dev-сборки — 0.0.0 / 1.
                versionName = providers.environmentVariable("APP_VERSION").getOrElse("0.0.0")
                versionCode = providers.environmentVariable("APP_BUILD").getOrElse("1").toInt()
            }
            buildFeatures { buildConfig = true }
            compileOptions { configureJava(this) }
            testOptions { configureUnitTests(this) }
            flavorDimensions += ENV_DIMENSION
            productFlavors {
                envFlavors().forEach { env ->
                    create(env.name) {
                        dimension = ENV_DIMENSION
                        env.properties.forEach { (key, value) -> buildConfigField("String", key, "\"$value\"") }
                    }
                }
            }
            buildTypes {
                debug { applicationIdSuffix = ".debug" }
                release {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                }
            }
            configureReleaseSigning(this)
        }

        // app — точка сбора покрытия всех Android-модулей: пол 70 % строк (08 §4.8), :lint — JVM, вне агрегата.
        configureKoverGateVariant()
        extensions.configure<KoverProjectExtension> {
            reports {
                variant(GATE_VARIANT) {
                    verify { rule { minBound(70) } }
                    filters {
                        excludes {
                            classes(
                                "*Hilt_*", "*_Factory*", "*_HiltModules*", "*_MembersInjector", "*_Impl",
                                "dagger.hilt.*", "hilt_aggregated_deps.*", "*ComposableSingletons*",
                                "*.BuildConfig", "*Preview*", "*NavEntry*",
                            )
                            annotatedBy("androidx.compose.ui.tooling.preview.Preview")
                        }
                    }
                }
            }
        }
        dependencies {
            // Листовые Android-модули: промежуточные :androidApp:core / :androidApp:feature пусты, :lint — JVM.
            rootProject.subprojects
                .filter {
                    it.path.startsWith(":androidApp:") && it.childProjects.isEmpty() && it != project && it.path != ":androidApp:lint"
                }
                .forEach { add("kover", project(it.path)) }
        }
    }
}
