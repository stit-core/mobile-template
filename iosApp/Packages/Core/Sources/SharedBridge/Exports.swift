// Единственная точка импорта Kotlin-фреймворка: фичи, Platform и App видят типы Shared через SharedBridge.
@_exported public import Shared

public typealias KotlinStateFlow = Kotlinx_coroutines_coreStateFlow
public typealias KotlinFlowCollector = Kotlinx_coroutines_coreFlowCollector
// Kotlin AppConfig под своим именем: в App рядом живёт одноимённый AppFoundation.AppConfig.
public typealias SharedAppConfig = AppConfig
