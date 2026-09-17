plugins {
    alias(libs.plugins.stit.android.library)
    alias(libs.plugins.stit.android.hilt)
}

android {
    namespace = "tech.stit.mobile.android.core.platform"
}

dependencies {
    implementation(projects.shared)

    testImplementation(projects.androidApp.core.testing)
}
