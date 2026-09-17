package tech.stit.mobile.core.push

// Провайдеры из DeviceRegisterInputBody.provider; приложение регистрирует все доступные (07 §6.1, 05 §2.5).
enum class PushProvider(val apiName: String) {
    Fcm("fcm"),
    Apns("apns"),
    Rustore("rustore"),
    Hms("hms"),
    Ntfy("ntfy"),
}

// Один транспорт — один провайдер: FcmTransport/RuStoreTransport на Android, ApnsRegistrar на iOS. token() == null — провайдер недоступен (нет GMS).
interface PushTransport {
    val provider: PushProvider

    suspend fun token(): String?
}

// Разрешение на уведомления (POST_NOTIFICATIONS / UNUserNotificationCenter): запрашивается в момент ценности, не на первом экране.
interface NotificationPermission {
    suspend fun isGranted(): Boolean

    suspend fun request(): Boolean
}
