plugins {
    alias(libs.plugins.stit.android.library)
    alias(libs.plugins.stit.android.compose)
    alias(libs.plugins.stit.roborazzi)
}

android {
    namespace = "tech.stit.mobile.android.core.ui"
}

dependencies {
    implementation(projects.androidApp.core.designsystem)
    implementation(projects.shared)

    testImplementation(projects.androidApp.core.testing)
}
