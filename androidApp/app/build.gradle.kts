plugins {
    alias(libs.plugins.stit.android.application)
    alias(libs.plugins.stit.android.compose)
    alias(libs.plugins.stit.android.hilt)
    alias(libs.plugins.stit.konsist)
}

android {
    namespace = "tech.stit.mobile.android"

    defaultConfig {
        applicationId = "tech.stit.mobile"
    }

    productFlavors {
        // demo ставится рядом с боевым приложением и в сеть не ходит (API_MODE=mock).
        getByName("demo") { applicationIdSuffix = ".demo" }
    }

    androidResources {
        // Per-app language: AGP собирает locale_config сам; localeFilters режут десятки локалей AppCompat/Play Services.
        generateLocaleConfig = true
        localeFilters += listOf("en", "ru")
    }
}

dependencies {
    implementation(projects.androidApp.core.designsystem)
    implementation(projects.androidApp.core.navigation)
    implementation(projects.androidApp.core.platform)
    implementation(projects.androidApp.core.ui)
    implementation(projects.androidApp.feature.auth)
    implementation(projects.androidApp.feature.profile)
    implementation(projects.androidApp.feature.settings)
    implementation(projects.androidApp.feature.shell)
    implementation(projects.androidApp.feature.todos)
    implementation(projects.androidApp.widget)
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.androidApp.core.testing)
    testImplementation(libs.kotlinx.serialization.json)

    lintChecks(projects.androidApp.lint)
}
