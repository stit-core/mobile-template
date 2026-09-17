plugins {
    alias(libs.plugins.stit.android.library)
}

android {
    namespace = "tech.stit.mobile.android.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.androidx.test.ext.junit)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.robolectric)
    api(libs.turbine)
}
