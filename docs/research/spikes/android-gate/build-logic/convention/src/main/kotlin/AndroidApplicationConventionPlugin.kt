import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import tech.stit.convention.configureJava
import tech.stit.convention.configureLint
import tech.stit.convention.configureUnitTests
import tech.stit.convention.int
import tech.stit.convention.libs

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("stit.quality")

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.int("compileSdk")
            compileSdkMinor = libs.int("compileSdkMinor")
            defaultConfig {
                minSdk = libs.int("minSdk")
                targetSdk = libs.int("targetSdk")
            }
            compileOptions { configureJava(this) }
            testOptions { configureUnitTests(this) }
            lint { configureLint(this) }
        }
    }
}
