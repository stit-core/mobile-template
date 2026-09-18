plugins {
    alias(libs.plugins.stit.android.library)
    alias(libs.plugins.stit.android.compose)
    alias(libs.plugins.stit.android.hilt)
    alias(libs.plugins.stit.roborazzi)
}

android {
    namespace = "tech.stit.mobile.android.widget"
}

dependencies {
    implementation(projects.androidApp.core.designsystem)
    implementation(projects.androidApp.core.platform)
    implementation(projects.shared)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.glance.appwidget)

    testImplementation(projects.androidApp.core.testing)
    testImplementation(libs.androidx.glance.appwidget.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
