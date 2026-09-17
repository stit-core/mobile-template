package tech.stit.mobile.core.lib

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Диспетчеры инжектируются, а не берутся из Dispatchers напрямую: тесты подставляют StandardTestDispatcher.
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

object DefaultDispatchers : DispatcherProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val io: CoroutineDispatcher get() = ioDispatcher
    override val default: CoroutineDispatcher get() = Dispatchers.Default
}

// Dispatchers.IO в commonMain недоступен для Apple-таргетов (internal в общих метаданных) — только через actual.
internal expect val ioDispatcher: CoroutineDispatcher
