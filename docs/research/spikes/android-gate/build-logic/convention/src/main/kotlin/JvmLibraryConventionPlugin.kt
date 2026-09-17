import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import tech.stit.convention.libs

/** Чистый Kotlin/JVM-модуль: домен без единой ссылки на Android SDK. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        // без этого Android Lint считает JVM-модуль внешней зависимостью и не смотрит внутрь
        pluginManager.apply("com.android.lint")
        pluginManager.apply("stit.quality")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        // Байткод домена потребляется Android-модулями — держим JVM_17, как у AGP.
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }

        dependencies {
            add("testImplementation", libs.findLibrary("junit4").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", libs.findLibrary("turbine").get())
        }
    }
}
