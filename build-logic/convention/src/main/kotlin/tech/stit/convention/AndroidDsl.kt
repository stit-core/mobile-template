package tech.stit.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.Lint
import com.android.build.api.dsl.TestOptions
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.util.Properties

// AGP 9 убрал compileOptions/testOptions/defaultConfig из Kotlin-видимости CommonExtension,
// поэтому общие куски — функциями поверх конкретных DSL-типов (06 L-06).

internal const val ENV_DIMENSION = "env"

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
    lint.lintConfig = rootProject.file("config/lint/lint.xml")
    lint.warningsAsErrors = true
    lint.abortOnError = true
    lint.checkDependencies = true
    // Baseline осознанно не заводим: шаблон стартует с нулём долгов (06 L-13).
}

internal class EnvFlavor(val name: String, val properties: Map<String, String>)

// Ось flavors `env` одна на все Android-модули: имена тасков (testDevDebugUnitTest, lintDevDebug)
// совпадают, и гейт не знает про модули. Список flavors = файлы config/env/<name>.properties.
internal fun Project.envFlavors(): List<EnvFlavor> {
    val files = rootProject.layout.projectDirectory.dir("config/env").asFile
        .listFiles { file -> file.extension == "properties" }
        .orEmpty()
        .sortedBy { it.name }
    check(files.isNotEmpty()) { "config/env пуст: нужен хотя бы один <flavor>.properties" }
    return files.map { file ->
        val properties = Properties().also { props -> file.inputStream().use(props::load) }
        EnvFlavor(file.nameWithoutExtension, properties.entries.associate { (key, value) -> key.toString() to value.toString() })
    }
}

// Ключ живёт вне репозитория (Vaultwarden); путь и пароли — только из env процесса сборки.
internal fun Project.configureReleaseSigning(extension: ApplicationExtension) = with(extension) {
    val keystorePath = providers.environmentVariable("STIT_KEYSTORE_PATH")
    signingConfigs.create("releaseLocal") {
        if (keystorePath.isPresent) {
            storeFile = file(keystorePath.get())
            storePassword = providers.environmentVariable("STIT_KEYSTORE_PASSWORD").getOrElse("")
            keyAlias = providers.environmentVariable("STIT_KEY_ALIAS").getOrElse("")
            keyPassword = providers.environmentVariable("STIT_KEY_PASSWORD").getOrElse("")
            enableV3Signing = true
            enableV4Signing = true
        }
    }
    buildTypes.getByName("release") {
        // Без STIT_KEYSTORE_* релиз подписывается debug-ключом: R8 проверяется в гейте без секретов.
        signingConfig = if (keystorePath.isPresent) signingConfigs.getByName("releaseLocal") else signingConfigs.getByName("debug")
    }
}
