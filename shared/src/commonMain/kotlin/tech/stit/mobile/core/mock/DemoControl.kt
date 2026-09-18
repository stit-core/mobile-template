package tech.stit.mobile.core.mock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Что про demo знает оболочка: восстановить состояние на старте, показывать ли баннер и как выйти. Внутри —
// ровно один из двух механизмов: сценарий из настроек в demo-сборке или runtime-переключатель в обычной.
internal class DemoControl(private val mode: DemoMode?, private val gate: DemoGate?) {
    val active: StateFlow<Boolean> = gate?.active ?: MutableStateFlow(false)

    suspend fun restore() {
        mode?.restore()
        gate?.restore()
    }

    suspend fun leave() {
        gate?.leave()
    }
}
