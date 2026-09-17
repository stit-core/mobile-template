plugins {
    alias(libs.plugins.stit.android.library)
    alias(libs.plugins.stit.android.hilt)
}

android {
    namespace = "tech.stit.mobiletemplate.core.data"
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
