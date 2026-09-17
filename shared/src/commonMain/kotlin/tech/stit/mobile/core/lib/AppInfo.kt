package tech.stit.mobile.core.lib

// Что приложение знает о себе и устройстве; собирает платформа (BuildConfig/Build на Android, Bundle/UIDevice на iOS).
data class AppInfo(
    val version: String,
    val build: Long,
    val osVersion: String,
    val deviceModel: String,
) {
    // Формат Sentry и DeviceRegisterInputBody.app_version: 1.4.2+1004002.
    val versionWithBuild: String get() = "$version+$build"
}
