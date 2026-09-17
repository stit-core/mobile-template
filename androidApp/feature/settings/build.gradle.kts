plugins {
    alias(libs.plugins.stit.android.feature)
}

android {
    namespace = "tech.stit.mobile.android.feature.settings"
}

dependencies {
    // Per-app locale ниже API 33 живёт в AppCompatDelegate (01 §3.5).
    implementation(libs.androidx.appcompat)
}
