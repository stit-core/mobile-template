plugins {
    alias(libs.plugins.stit.android.library)
    alias(libs.plugins.stit.android.hilt)
}

android {
    namespace = "tech.stit.mobile.android.core.platform"
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.rustore.pushclient)
    implementation(libs.sentry.android)

    testImplementation(projects.androidApp.core.testing)
    testImplementation(libs.androidx.work.testing)

    ksp(libs.androidx.appfunctions.compiler)
}
