package tech.stit.mobile.core.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

// Презентер владеет scope (закрывает платформа в onCleared/deinit); SupervisorJob — одна упавшая корутина не гасит остальные.
internal fun presenterScope(dispatchers: DispatcherProvider): CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main)
