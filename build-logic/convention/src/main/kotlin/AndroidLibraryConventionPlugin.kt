import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import tech.stit.convention.ENV_DIMENSION
import tech.stit.convention.configureJava
import tech.stit.convention.configureKoverGateVariant
import tech.stit.convention.configureUnitTests
import tech.stit.convention.envFlavors
import tech.stit.convention.int
import tech.stit.convention.libs

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        // NavKey Navigation 3 переживают смерть процесса через kotlinx.serialization — плагин нужен модулю с маршрутами.
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        pluginManager.apply("stit.android.lint")
        pluginManager.apply("stit.quality")

        extensions.configure<LibraryExtension> {
            compileSdk = libs.int("compileSdk")
            compileSdkMinor = libs.int("compileSdkMinor")
            defaultConfig { minSdk = libs.int("minSdk") }
            // Префикс ресурсов из пути модуля (:androidApp:core:ui → core_ui_): Lint ResourceName следит.
            resourcePrefix = path.removePrefix(":androidApp:").replace(':', '_') + "_"
            compileOptions { configureJava(this) }
            testOptions { configureUnitTests(this) }
            flavorDimensions += ENV_DIMENSION
            productFlavors {
                envFlavors().forEach { env -> create(env.name) { dimension = ENV_DIMENSION } }
            }
        }
        configureKoverGateVariant()
    }
}
