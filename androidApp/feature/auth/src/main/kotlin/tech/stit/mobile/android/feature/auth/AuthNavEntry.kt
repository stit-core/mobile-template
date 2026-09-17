package tech.stit.mobile.android.feature.auth

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import tech.stit.mobile.android.core.navigation.Destination

// Регистрация экранов фичи в NavDisplay приложения; переходы наружу — через колбэки, feature не знает других feature.
fun EntryProviderScope<NavKey>.authEntries(onOpenSettings: () -> Unit, onBack: () -> Unit) {
    entry<Destination.Login> { LoginRoute(onOpenSettings = onOpenSettings) }
    entry<Destination.Sessions> { SessionsRoute(onBack = onBack) }
    entry<Destination.DeleteAccount> { DeleteAccountRoute(onBack = onBack) }
}
