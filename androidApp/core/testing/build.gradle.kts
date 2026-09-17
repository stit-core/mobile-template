plugins {
    alias(libs.plugins.stit.android.library)
    alias(libs.plugins.stit.android.compose)
}

android {
    namespace = "tech.stit.mobile.android.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.androidx.test.ext.junit)
    api(libs.junit4)
    api(libs.kotlin.test.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.robolectric)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
    api(libs.roborazzi.junit.rule)
    api(libs.turbine)

    implementation(projects.androidApp.core.designsystem)
    implementation(projects.androidApp.core.platform)
    implementation(projects.shared)
}
