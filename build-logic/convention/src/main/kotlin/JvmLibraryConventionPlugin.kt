import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import tech.stit.convention.library
import tech.stit.convention.libs

// Чистый Kotlin/JVM-модуль (например, :androidApp:lint): без Android SDK, но под тем же Lint и качеством.
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        // Без standalone-плагина Lint считает JVM-модуль внешней зависимостью и не смотрит внутрь (06 L-11).
        pluginManager.apply("com.android.lint")
        pluginManager.apply("stit.android.lint")
        pluginManager.apply("stit.quality")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        // Байткод потребляется Android-модулями — держим JVM 17, как у AGP.
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }

        dependencies {
            add("testImplementation", libs.library("junit4"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
        }
    }
}
