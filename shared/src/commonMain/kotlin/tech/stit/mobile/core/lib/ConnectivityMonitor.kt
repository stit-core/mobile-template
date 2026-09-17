package tech.stit.mobile.core.lib

import kotlinx.coroutines.flow.StateFlow

// Сеть есть/нет — от платформы (ConnectivityManager / NWPathMonitor); презентеры переключают offline-ветки по этому флагу.
interface ConnectivityMonitor {
    val online: StateFlow<Boolean>
}
