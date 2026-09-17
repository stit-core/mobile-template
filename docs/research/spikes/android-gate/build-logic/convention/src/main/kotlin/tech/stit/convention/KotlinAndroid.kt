package tech.stit.convention

import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.Lint
import com.android.build.api.dsl.TestOptions
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.int(alias: String): Int = findVersion(alias)
    .orElseThrow { IllegalStateException("нет версии '$alias' в libs.versions.toml") }
    .requiredVersion
    .toInt()

// AGP 9 убрал compileOptions/lint/testOptions/defaultConfig из Kotlin-видимости
// CommonExtension (new DSL), поэтому общие куски — отдельными функциями поверх
// конкретных DSL-интерфейсов; вызываются из ApplicationExtension/LibraryExtension.

internal fun configureJava(options: CompileOptions) {
    options.sourceCompatibility = JavaVersion.VERSION_17
    options.targetCompatibility = JavaVersion.VERSION_17
}

internal fun configureUnitTests(options: TestOptions) {
    options.unitTests.apply {
        // Robolectric/Roborazzi: без этого нет доступа к res/assets в JVM-тестах
        isIncludeAndroidResources = true
        isReturnDefaultValues = true
    }
}

internal fun Project.configureLint(lint: Lint) {
    lint.apply {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        xmlReport = true
        sarifReport = true
        htmlReport = false
        textReport = true
        // baseline осознанно НЕ заводим: шаблон стартует с нулём долгов.
        // Для миграции существующего кода — `./gradlew updateLintBaseline`.
        disable += "GradleDependency"
    }
}
