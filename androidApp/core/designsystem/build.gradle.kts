plugins {
    alias(libs.plugins.stit.android.library)
    alias(libs.plugins.stit.android.compose)
    alias(libs.plugins.stit.roborazzi)
}

android {
    namespace = "tech.stit.mobile.android.core.designsystem"
}

dependencies {
    testImplementation(projects.androidApp.core.testing)
}
