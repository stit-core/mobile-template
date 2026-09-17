plugins {
    alias(libs.plugins.stit.android.library)
    alias(libs.plugins.stit.android.compose)
}

android {
    namespace = "tech.stit.mobile.android.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)

    implementation(projects.shared)
    implementation(libs.kotlinx.serialization.core)

    testImplementation(projects.androidApp.core.testing)
}
