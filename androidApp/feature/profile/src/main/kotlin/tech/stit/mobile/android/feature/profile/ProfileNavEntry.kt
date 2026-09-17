package tech.stit.mobile.android.feature.profile

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import tech.stit.mobile.android.core.navigation.Destination

fun EntryProviderScope<NavKey>.profileEntries(onOpenSessions: () -> Unit, onOpenDeleteAccount: () -> Unit) {
    entry<Destination.Profile> { ProfileRoute(onOpenSessions = onOpenSessions, onOpenDeleteAccount = onOpenDeleteAccount) }
}
