package tech.stit.mobile.android.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import tech.stit.mobile.core.deeplink.Route

// NavKey-обёртки над Route из shared (rule navigation): deep-link-маршруты плюс экраны, недостижимые по ссылке.
@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Login : Destination

    @Serializable
    data object TodoList : Destination

    @Serializable
    data class TodoDetail(val id: String) : Destination

    @Serializable
    data object Profile : Destination

    @Serializable
    data object Sessions : Destination

    @Serializable
    data object DeleteAccount : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object PushOptIn : Destination

    // Корни вкладок: с них back закрывает приложение, на них не копится стек.
    val isTopLevel: Boolean
        get() = this == TodoList || this == Profile || this == Settings
}

fun Route.toDestination(): Destination = when (this) {
    Route.Home, Route.TodoList -> Destination.TodoList
    Route.Login -> Destination.Login
    Route.Profile -> Destination.Profile
    Route.Settings -> Destination.Settings
    is Route.TodoDetail -> Destination.TodoDetail(id)
}
