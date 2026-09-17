import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import tech.stit.convention.configureLint

// Один профиль Android Lint на все модули; severity-переопределения — config/lint/lint.xml.
class AndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { lint { configureLint(this) } }
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { lint { configureLint(this) } }
        }
        // Чистый JVM-модуль: standalone-плагин com.android.lint регистрирует extension `lint` на проекте.
        pluginManager.withPlugin("com.android.lint") {
            extensions.findByType<Lint>()?.let { configureLint(it) }
        }
    }
}
