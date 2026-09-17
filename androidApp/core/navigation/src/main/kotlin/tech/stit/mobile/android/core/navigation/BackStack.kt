package tech.stit.mobile.android.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

// Один back stack на приложение: вкладка — замена корня, экран — push без дубля сверху, back — pop не ниже корня.

fun NavBackStack<NavKey>.navigateTo(destination: Destination) {
    if (lastOrNull() != destination) add(destination)
}

fun NavBackStack<NavKey>.resetTo(destination: Destination) {
    if (size != 1 || first() != destination) {
        clear()
        add(destination)
    }
}

fun NavBackStack<NavKey>.pop(): Boolean {
    if (size <= 1) return false
    removeAt(size - 1)
    return true
}

val NavBackStack<NavKey>.current: Destination?
    get() = lastOrNull() as? Destination
