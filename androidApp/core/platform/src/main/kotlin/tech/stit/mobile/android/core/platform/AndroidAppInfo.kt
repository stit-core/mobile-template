package tech.stit.mobile.android.core.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.lib.Store

fun Context.androidAppInfo(): AppInfo {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    return AppInfo(
        appId = packageName,
        version = info.versionName ?: "0.0.0",
        build = PackageInfoCompat.getLongVersionCode(info),
        osVersion = "Android ${Build.VERSION.RELEASE}",
        deviceModel = Build.MODEL,
        store = installedStore(),
    )
}

// Канал установки для GET /api/app/requirements?store= — по установщику, не по flavor: один APK живёт в нескольких магазинах.
internal fun Context.installedStore(): Store {
    // Системе может быть неизвестен установщик (sideload, тестовое окружение) — это Direct, а не ошибка.
    val installer = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    }.getOrNull()
    return when (installer) {
        "com.android.vending" -> Store.Play
        "ru.vk.store" -> Store.RuStore
        "com.huawei.appmarket" -> Store.AppGallery
        else -> Store.Direct
    }
}
