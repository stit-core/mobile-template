package tech.stit.mobile.core.deeplink

// Маршруты приложения — sealed, без строк: Android оборачивает в NavKey, iOS кладёт в NavigationStack(path:).
sealed class Route {
    data object Home : Route()

    data object Login : Route()

    data object Profile : Route()

    data object Settings : Route()

    data object TodoList : Route()

    data class TodoDetail(val id: String) : Route()

    // Настройки и правовые документы доступны до логина (05 L-3); остальное ждёт сессию как pending route.
    val requiresAuth: Boolean
        get() = when (this) {
            Login, Settings -> false
            Home, Profile, TodoList, is TodoDetail -> true
        }
}
