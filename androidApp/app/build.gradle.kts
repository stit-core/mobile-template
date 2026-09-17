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
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    testImplementation(projects.androidApp.core.testing)

    lintChecks(projects.androidApp.lint)
}
