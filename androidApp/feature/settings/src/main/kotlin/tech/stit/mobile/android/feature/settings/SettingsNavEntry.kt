package tech.stit.mobile.android.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import tech.stit.mobile.android.core.navigation.Destination

fun EntryProviderScope<NavKey>.settingsEntries(onOpenNotifications: () -> Unit, onOpenLink: (String) -> Unit, onBack: () -> Unit) {
    entry<Destination.Settings> { SettingsRoute(onOpenNotifications = onOpenNotifications, onOpenLink = onOpenLink) }
    entry<Destination.PushOptIn> { PushOptInRoute(onBack = onBack) }
}
