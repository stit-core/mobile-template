import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "tech.stit.mobile.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.binary.compatibility.validator.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
}

// Идентификаторы плагинов == алиасы stit-* в libs.versions.toml: модуль пишет
// alias(libs.plugins.stit.android.feature) и не знает про classpath.
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "stit.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "stit.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "stit.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "stit.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "stit.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidLint") {
            id = "stit.android.lint"
            implementationClass = "AndroidLintConventionPlugin"
        }
        register("kmpLibrary") {
            id = "stit.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = "stit.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("quality") {
            id = "stit.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("roborazzi") {
            id = "stit.roborazzi"
            implementationClass = "RoborazziConventionPlugin"
        }
        register("konsist") {
            id = "stit.konsist"
            implementationClass = "KonsistConventionPlugin"
        }
    }
}
