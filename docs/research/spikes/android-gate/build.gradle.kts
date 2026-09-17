plugins {
    // Версии плагинов пинятся здесь (apply false) — convention-плагины из build-logic
    // применяют их по id, не зная про classpath.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.roborazzi) apply false

    alias(libs.plugins.kover)
    alias(libs.plugins.spotless)
    // dependency-analysis 3.19.2 знает AGP только до 9.3.1 и на 9.4.0 не
    // регистрирует project-health — держим вне гейта до совместимой версии.
    // alias(libs.plugins.dependency.analysis)
}

dependencies {
    kover(projects.app)
    kover(projects.core.model)
    kover(projects.core.data)
    kover(projects.core.designsystem)
    kover(projects.feature.todos)
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*Hilt_*",
                    "*_Factory*",
                    "*_HiltModules*",
                    "*_MembersInjector",
                    "*ComposableSingletons*",
                    "*_Impl",
                    "dagger.hilt.*",
                    "hilt_aggregated_deps.*",
                    "*.BuildConfig",
                    "*Preview*",
                )
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }
        verify {
            rule("покрытие строк доменных модулей") {
                bound {
                    minValue = 60
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                }
            }
        }
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/generated/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}
