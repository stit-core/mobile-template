package tech.stit.mobile.core.lib

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Сеть есть/нет — от платформы (ConnectivityManager / NWPathMonitor); презентеры переключают offline-ветки по этому флагу.
interface ConnectivityMonitor {
    val online: StateFlow<Boolean>
}

// Holder для платформ: Swift не реализует Kotlin StateFlow сам, а обновляет этот объект из NWPathMonitor.
class ConnectivityState(initial: Boolean = true) : ConnectivityMonitor {
    private val state = MutableStateFlow(initial)
    override val online: StateFlow<Boolean> = state.asStateFlow()

    fun update(online: Boolean) {
        state.value = online
    }
}
