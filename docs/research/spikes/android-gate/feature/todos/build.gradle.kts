plugins {
    alias(libs.plugins.stit.android.feature)
}

android {
    namespace = "tech.stit.mobiletemplate.feature.todos"
}

dependencies {
    implementation(projects.core.data)
    implementation(libs.kotlinx.coroutines.core)
}
