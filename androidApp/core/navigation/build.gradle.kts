plugins {
    alias(libs.plugins.stit.android.library)
    alias(libs.plugins.stit.android.compose)
}

android {
    namespace = "tech.stit.mobile.android.core.navigation"
}

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    testImplementation(projects.androidApp.core.testing)
}
