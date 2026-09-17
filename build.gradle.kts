plugins {
    // Версии плагинов пинятся здесь (apply false) — convention-плагины из build-logic
    // применяют их по id, не зная про classpath.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.sort.dependencies) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.binary.compatibility.validator) apply false

    // Корень — тоже модуль качества: Spotless для корневых *.kts и build-logic.
    alias(libs.plugins.stit.quality)
}
