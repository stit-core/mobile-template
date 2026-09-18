package tech.stit.mobile.core.background

import kotlin.time.Instant

// Требование продукта к фоновой работе (словарь карты mobile/<surface>/background-tasks.md); механизм под него выбирает платформа.
data class BackgroundTask(
    val id: String,
    val trigger: BackgroundTrigger,
    val timing: BackgroundTiming,
    val conditions: List<BackgroundCondition> = emptyList(),
    // Periodic: как часто повторять. Time: момент, к которому нужен показ.
    val periodMinutes: Long = 0,
    val at: Instant? = null,
)

enum class BackgroundTrigger {
    Time,
    Periodic,
    Connectivity,
    RemoteEvent,
    UserStarted,
}

// Exact обещает момент показа уведомления, не момент исполнения кода: точнее Window система не исполняет.
enum class BackgroundTiming {
    Exact,
    Window,
    Opportunistic,
    Continuous,
}

enum class BackgroundCondition {
    Network,
    Unmetered,
    Charging,
}
