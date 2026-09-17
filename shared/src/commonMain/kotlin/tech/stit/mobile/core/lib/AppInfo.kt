package tech.stit.mobile.core.lib

// Что приложение знает о себе и устройстве; собирает платформа (BuildConfig/Build на Android, Bundle/UIDevice на iOS).
data class AppInfo(
    // applicationId / bundle id — первый сегмент User-Agent (07 §2.6).
    val appId: String,
    val version: String,
    val build: Long,
    // «Android 16» / «iOS 27.0» — имя ОС и версия, как в User-Agent.
    val osVersion: String,
    val deviceModel: String,
    val store: Store,
) {
    // Формат Sentry и DeviceRegisterInputBody.app_version: 1.4.2+1004002.
    val versionWithBuild: String get() = "$version+$build"
}

// Канал установки для GET /api/app/requirements?store=; платформа выводит его из установщика, а не из флейвора сборки.
enum class Store(val apiName: String) {
    // Android: PackageManager.getInstallSourceInfo().installingPackageName == "com.android.vending".
    Play("play"),

    // "ru.vk.store".
    RuStore("rustore"),

    // "com.huawei.appmarket".
    AppGallery("appgallery"),

    // iOS всегда: другого канала у платформы нет.
    AppStore("appstore"),

    // Установщик неизвестен: APK из Gitea Release или sideload.
    Direct("direct"),
}
