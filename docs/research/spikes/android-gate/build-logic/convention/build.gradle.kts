import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "tech.stit.mobiletemplate.buildlogic"

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
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
}

// Идентификаторы плагинов == алиасы в libs.versions.toml (stit-*), чтобы модули
// писали alias(libs.plugins.stit.android.feature) и не знали про classpath.
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "stit.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "stit.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "stit.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "stit.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "stit.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = "stit.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidHilt") {
            id = "stit.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("quality") {
            id = "stit.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("androidRoborazzi") {
            id = "stit.android.roborazzi"
            implementationClass = "AndroidRoborazziConventionPlugin"
        }
    }
}
