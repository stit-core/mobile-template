# NF-02 — feature-модуль зависит от другого feature-модуля: ловит stit.android.feature при конфигурации (Konsist — второй рубеж)
EXPECT="android unit tests"
TOUCHES="androidApp/feature/settings/build.gradle.kts"
apply() {
    printf '\ndependencies {\n    implementation(projects.androidApp.feature.shell)\n}\n' >>"$TOUCHES"
}
