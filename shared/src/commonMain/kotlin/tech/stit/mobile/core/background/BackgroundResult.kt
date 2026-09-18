package tech.stit.mobile.core.background

// Что увидит пользователь после фоновой работы; ошибки раннер не бросает, а возвращает Rejected — платформа решает про повтор.
sealed class BackgroundResult {
    // Локальное уведомление тем же payload'ом, что push: ключи текста и категория — из реестра платформы.
    data class Notify(
        val category: String,
        val titleKey: String,
        val bodyKey: String,
        val args: List<String>,
        val deepLink: String?,
        val notificationId: String,
    ) : BackgroundResult()

    data class RefreshWidget(val slug: String) : BackgroundResult()

    data object FreshData : BackgroundResult()

    data class Rejected(val reason: BackgroundRejection) : BackgroundResult()
}

enum class BackgroundRejection {
    Offline,
    SignedOut,
    NothingToDo,
    Failed,
}
