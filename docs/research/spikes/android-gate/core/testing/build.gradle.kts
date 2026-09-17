plugins {
    alias(libs.plugins.stit.android.library)
}

android {
    namespace = "tech.stit.mobiletemplate.core.testing"
}

dependencies {
    api(projects.core.model)
    api(projects.core.data)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    implementation(libs.kotlinx.coroutines.core)
}
