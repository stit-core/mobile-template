plugins {
    alias(libs.plugins.stit.android.library.compose)
}

android {
    namespace = "tech.stit.mobiletemplate.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
}
